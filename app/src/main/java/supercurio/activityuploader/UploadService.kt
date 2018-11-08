package supercurio.activityuploader

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import supercurio.activityuploader.stravawebapi.StravaUpload
import supercurio.activityuploader.stravawebapi.StravaUploadProgress
import supercurio.activityuploader.stravawebapi.StravaWebApi
import java.io.File

class UploadService : Service() {

    private val binder: IBinder = ServiceBinder()

    private val queue = UploadQueue(applicationContext)
    internal val itemsProgress = mutableListOf<StravaUploadProgress>()

    private val serviceJob = Job()
    private val bgScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class ServiceBinder : Binder() {
        fun getService(): UploadService {
            return this@UploadService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        serviceJob.cancel()
    }

    internal fun uploadQueue() {
        itemsProgress.clear()

        bgScope.launch {
            val service = StravaWebApi.getService(applicationContext)

            queue.list().forEach { queueItem ->
                bgScope.launch {
                    StravaUpload.uploadActivityFile(service,
                                                    applicationContext,
                                                    File(queueItem.fileName)) { progress ->
                        itemsProgress += progress
                        Log.i(TAG, progress.toString())
                        broadcastProgressToUi()
                    }
                }
            }
        }
    }


    private fun broadcastProgressToUi() {
        sendBroadcast(Intent(Constants.INTENT_ACTION_UPLOADS_UI_UPDATE))
    }

    companion object {
        private const val TAG = "UploadService"
    }
}
