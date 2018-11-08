package supercurio.activityuploader

import okhttp3.logging.HttpLoggingInterceptor

/**
 * Created by François Simond on 8/17/18.
 * Part of activity-uploader
 */
object Constants {
    internal const val INTENT_ACTION_UPLOAD_FILES = "UPLOAD_FILES"
    internal const val INTENT_UPLOAD_FILES_EXTRAS = "UPLOAD_FILES_EXTRAS"

    internal const val INTENT_ACTION_UPLOADS_UI_UPDATE = "UPLOAD_UI_UPDATE"

    internal const val GZ_UPLOADS = true

    val HTTP_LOGGING_LEVEL = HttpLoggingInterceptor.Level.BODY
}

