package supercurio.activityuploader.activities

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.codekidlabs.storagechooser.StorageChooser
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import supercurio.activityuploader.Constants
import supercurio.activityuploader.R
import supercurio.activityuploader.fitbitwebapi.FitbitAuth
import supercurio.activityuploader.stravawebapi.*
import java.io.File


class MainActivity : AppCompatActivity() {

    // TODO: Move account management in a settings page elsewhere
    // TODO: Implement the planned UI

    private val activityJob = Job()
    private val bgScope = CoroutineScope(Dispatchers.IO + activityJob)

    private lateinit var customTabSession: CustomTabsSession

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_STORAGE = 1
        // TODO: also support Firefox
        private const val CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CustomTabsClient.bindCustomTabsService(this,
                                               CUSTOM_TAB_PACKAGE_NAME,
                                               customTabServiceConnection)

        handleAuthCallbacks()

        // TODO: ask the user only when necessary
        if (ContextCompat.checkSelfPermission(this@MainActivity,
                                              Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            Log.w(TAG, "Storage permission not granted")

            // No explanation needed, we can request the permission.
            ActivityCompat
                    .requestPermissions(this@MainActivity,
                                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                        PERMISSION_REQUEST_STORAGE)
        }

        StravaAthlete.load(this)?.let { showAthleteInfo(it) }

        //
        // Buttons
        //

        buttonMyConnectWithStrava.setOnClickListener {
            val intent = CustomTabsIntent.Builder(customTabSession)
                    .setShowTitle(true)
                    .setToolbarColor(StravaConfig.CUSTOM_TAB_COLOR)
                    .build()
            intent.launchUrl(this, StravaAuth.buildAuthUri(this))
        }

        buttonMyDisconnectFromStrava.setOnClickListener {
            bgScope.launch {
                StravaWebApi.deAuthorize(applicationContext)
            }

            val intent = CustomTabsIntent.Builder(customTabSession)
                    .setShowTitle(true)
                    .setToolbarColor(StravaConfig.CUSTOM_TAB_COLOR)
                    .build()

            intent.launchUrl(this, "https://www.strava.com/logout".toUri())
        }

        buttonConnectToFitbit.setOnClickListener {
            val intent = CustomTabsIntent.Builder(customTabSession)
                    .setShowTitle(true)
                    .build()
            intent.launchUrl(this, FitbitAuth.buildAuthUri(this))
        }

        buttonPickFiles.setOnClickListener {
            @Suppress("DEPRECATION")
            val chooser = StorageChooser.Builder()
                    .withActivity(this@MainActivity)
                    .withFragmentManager(fragmentManager)
                    .allowCustomPath(true)
                    .setType(StorageChooser.FILE_PICKER)
                    .shouldResumeSession(true)
                    .customFilter(arrayListOf("gpx", "GPX",
                                              "tcx", "TCX",
                                              "fit", "FIT"))
                    .build()

            chooser.show()

            chooser.setOnSelectListener { fileName ->
                Log.i(TAG, "Selected: $fileName")
                startUploadActivity(arrayListOf(fileName))
            }
            chooser.setOnMultipleSelectListener { fileNames ->
                Log.i(TAG, "Selected multiple: $fileNames")
                startUploadActivity(fileNames)
            }
            chooser.setOnCancelListener { }
        }

        buttonUploadFile.setOnClickListener {
            bgScope.launch {
                val service = StravaWebApi.getService(applicationContext)
                StravaUpload.uploadActivityFile(service,
                                                applicationContext,
                                                File("/sdcard/test.tcx")) { progress ->
                    Log.i(TAG, "Upload Progress: $progress")
                }
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_STORAGE -> {
                if (grantResults.isNotEmpty() &&
                        grantResults.first() == PackageManager.PERMISSION_DENIED) {

                    // TODO: find a good way to inform the user
                    Log.e(TAG, "Storage permission is required")
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        activityJob.cancel()

        super.onDestroy()
        unbindService(customTabServiceConnection)
    }

    private fun startUploadActivity(files: ArrayList<String>) {
        Intent(this@MainActivity, UploadViaServiceActivity::class.java).apply {
            action = Constants.INTENT_ACTION_UPLOAD_FILES
            putStringArrayListExtra(Constants.INTENT_UPLOAD_FILES_EXTRAS, files)
        }

        startActivity(intent)
    }

    private fun showAthleteInfo(athlete: StravaAthlete) {
        var text = ""
        athlete.firstName.apply { text += this }
        athlete.lastName.apply { text += " " + this }

        tvUserInfo.text = text
        buttonOpenStravaProfileUrl.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = "https://www.strava.com/athletes/${athlete.id}".toUri()
            startActivity(intent)
        }

        Picasso.get().load(athlete.profilePictureUrl).into(imageUserProfile)
    }

    private val customTabServiceConnection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(componentName: ComponentName,
                                                  client: CustomTabsClient) {
            Log.i(TAG, "onCustomTabsServiceConnected")
            customTabSession = client.newSession(customTabCallback)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected")
        }
    }

    private fun handleAuthCallbacks() {
        if (intent.action != Intent.ACTION_VIEW) return
        val callbackUri = intent.data ?: return

        Log.i(TAG, "callback URI path: ${callbackUri.path}")

        val path = callbackUri.path ?: return

        bgScope.launch {
            when (path) {
                getString(R.string.oauth_callback_strava_path) -> {
                    StravaAuth.fromCallbackUri(this@MainActivity, callbackUri)?.let {
                        runOnUiThread { showAthleteInfo(it) }
                    }
                }

                getString(R.string.oauth_callback_fitbit_path) ->
                    FitbitAuth.fromCallbackUri(this@MainActivity, callbackUri)
            }
        }
    }

    private val customTabCallback = object : CustomTabsCallback() {
        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {

            Log.d(TAG, "Navigation event: " +
                    when (navigationEvent) {
                        CustomTabsCallback.NAVIGATION_ABORTED -> "NAVIGATION_ABORTED"
                        CustomTabsCallback.NAVIGATION_FAILED -> "NAVIGATION_FAILED"
                        CustomTabsCallback.NAVIGATION_FINISHED -> "NAVIGATION_FINISHED"
                        CustomTabsCallback.NAVIGATION_STARTED -> "NAVIGATION_STARTED"
                        CustomTabsCallback.TAB_HIDDEN -> "TAB_HIDDEN"
                        CustomTabsCallback.TAB_SHOWN -> "TAB_SHOWN"
                        else -> "Unsupported"
                    })

            extras?.keySet()?.forEach { key ->
                Log.d(TAG, "event extra: $key: " + extras[key])
            }
        }
    }
}
