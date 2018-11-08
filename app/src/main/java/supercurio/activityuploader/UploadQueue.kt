package supercurio.activityuploader

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import java.io.File

class UploadQueue(context: Context) : ContextWrapper(context) {

    private fun getPrefs() = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun add(fileName: String) {
        add(fileName, StravaActivityType.fromFileName(fileName))
    }

    fun add(uri: Uri) {
        val tempFile = File.createTempFile("gps-track", null, cacheDir)
        val contentIs = contentResolver.openInputStream(uri)
        if (contentIs == null) {
            Log.e(TAG, "Unable to access $uri content")
            return
        }

        contentIs.copyTo(tempFile.outputStream())
        contentIs.close()

        val path = uri.path
        if (path == null)
            add(tempFile.absolutePath)
        else
            add(tempFile.absolutePath, StravaActivityType.fromFileName(path))
    }

    fun add(fileName: String, activityType: StravaActivityType) {
        getPrefs().edit {
            putString(fileName, activityType.name)
        }
    }

    fun list(): List<UploadQueueItem> =
            getPrefs().all.map {
                UploadQueueItem(it.key,
                                StravaActivityType.valueOf(it.value.toString()))
            }


    fun remove(fileName: String) {
        File(fileName).delete()
        getPrefs().edit {
            remove(fileName)
        }
    }

    companion object {
        private const val TAG = "UploadQueue"
        private const val PREF_NAME = "upload_queue"
    }
}

data class UploadQueueItem(
        val fileName: String,
        val activityType: StravaActivityType
)

enum class StravaActivityType {
    RIDE,
    RUN,
    SWIM,
    WORKOUT,
    HIKE,
    WALK,
    NORDICSKI,
    ALPINESKI,
    BACKCOUNTRYSKI,
    ICESKATE,
    INLINESKATE,
    KITESURF,
    ROLLERSKI,
    WINDSURF,
    SNOWBOARD,
    SNOWSHOE,
    EBIKERIDE,
    VIRTUALRIDE, ;

    companion object {
        fun fromFileName(fileName: String) = when {
            // Gadgetbridge GPX files
            fileName.contains("-running-") -> RUN
            fileName.contains("-biking-") -> RIDE
            fileName.contains("-walking-") -> WALK
            else -> RUN
        }
    }

    override fun toString(): String {
        return name.toLowerCase()
    }
}
