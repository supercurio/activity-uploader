package supercurio.activityuploader.activities

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_upload_shared_file.*
import kotlinx.android.synthetic.main.upload_list_item.view.*
import supercurio.activityuploader.Constants
import supercurio.activityuploader.R
import supercurio.activityuploader.UploadQueue
import supercurio.activityuploader.UploadService
import supercurio.activityuploader.stravawebapi.StravaUploadProgress

class UploadViaServiceActivity : AppCompatActivity() {

    // TODO: implement the planned UI

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private val uploadQueue = UploadQueue(applicationContext)
    private var uploadService: UploadService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_shared_file)

        viewManager = LinearLayoutManager(this)
        viewAdapter = UploadsAdapter(this)

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        when (intent.action) {
            Constants.INTENT_ACTION_UPLOAD_FILES -> {
                val files = intent.getStringArrayListExtra(Constants.INTENT_UPLOAD_FILES_EXTRAS)
                Log.i(TAG, "files: $files")
                files.forEach { uploadQueue.add(it) }
            }

            Intent.ACTION_SEND -> {
                Log.i(TAG, "Receive One")

                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                    uploadQueue.add(uri)
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                Log.i(TAG, "Receive Multiple")
                Log.i(TAG, intent.toString())

                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                    it.forEach { uri -> uploadQueue.add(uri) }
                }
            }
        }

        bindService(Intent(this, UploadService::class.java),
                    serviceConnection,
                    Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(serviceUpdatesReceiver,
                         IntentFilter(Constants.INTENT_ACTION_UPLOADS_UI_UPDATE))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(serviceUpdatesReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    inner class UploadsAdapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount(): Int = uploadService?.itemsProgress?.size ?: 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                ViewHolder(
                        LayoutInflater.from(context).inflate(R.layout.upload_list_item,
                                                             parent,
                                                             false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uploadProgress = uploadService?.itemsProgress?.get(position) ?: return
            holder.tvName.text = uploadProgress.name

            val uploadText = when (uploadProgress) {
                is StravaUploadProgress.UploadProcessingCompleted -> {
                    "Finished: Status: " +
                            "${uploadProgress.upload.status}, " +
                            "Activity Id: ${uploadProgress.upload.activityId}"
                }
                is StravaUploadProgress.UploadFailure ->
                    "Failure: ${uploadProgress.upload400error.error}"

                is StravaUploadProgress.UploadProcessingTimeout -> "Timeout"
                is StravaUploadProgress.Uploaded -> "Uploaded: " + uploadProgress.upload.status
                is StravaUploadProgress.Preparing -> "Preparing"
            }

            holder.tvCompletion.text = uploadText
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        internal val tvName = view.uploadName
        internal val tvCompletion = view.uploadCompletion
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as UploadService.ServiceBinder
            val uploadService = binder.getService()

            uploadService.uploadQueue()

            this@UploadViaServiceActivity.uploadService = uploadService
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            uploadService = null
        }
    }


    private val serviceUpdatesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (uploadService == null) return

            viewAdapter.notifyDataSetChanged()
        }
    }

    companion object {
        private const val TAG = "UploadShared"
    }
}
