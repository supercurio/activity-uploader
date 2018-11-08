package supercurio.activityuploader.stravawebapi

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import supercurio.activityuploader.Constants
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPOutputStream

data class StravaUpload(

        @SerializedName("id") val id: Long,
        @SerializedName("external_id") val externalId: String,
        @SerializedName("error") val error: StravaUploadError,
        @SerializedName("status") val status: String,
        @SerializedName("activity_id") val activityId: Long?

) {
    companion object {
        private const val TAG = "StravaUpload"

        private val MEDIA_TYPE_TEXT_PLAIN by lazy { MediaType.parse("text/plain") }

        // Should be ran as low-priority
        suspend fun uploadActivityFile(service: StravaService,
                                       context: Context,
                                       inputFile: File,
                                       compressUncompressed: Boolean = Constants.GZ_UPLOADS,
                                       progressCallback: (StravaUploadProgress) -> Unit) {

            val name = inputFile.name

            val type = UploadDataType.fromFileExtension(inputFile.name, compressUncompressed)
                    ?: TODO("add error management for unsupported files")

            val uploadDataType = RequestBody.create(MEDIA_TYPE_TEXT_PLAIN, type.toString())

            // If uploading in a compressed format gzip the input to a temporary file
            val fileToUpload = if (compressUncompressed) {
                val tempFile = File.createTempFile("gps-track", null, context.cacheDir)
                val gzFile = copyToGz(inputFile.inputStream(), tempFile)
                // TODO: delete input file if it's our app
                // inputFile.delete()
                gzFile
            } else
                inputFile

            // create the form data
            val fileRequestBody = RequestBody.create(
                    MediaType.parse("application/octet-stream"),
                    fileToUpload)
            val fileMultipartBody = MultipartBody.Part.createFormData(
                    "file",
                    fileToUpload.absolutePath,
                    fileRequestBody)

            // actually upload the file to Strava's API
            val uploadResponse = service.uploadActivity(fileMultipartBody,
                                                        uploadDataType).await()

            // handle the upload's result
            if (uploadResponse.isSuccessful) {
                uploadResponse.body()?.let { upload ->
                    progressCallback(StravaUploadProgress.Uploaded(name, upload))

                    checkProgress(service, progressCallback, upload)
                    return
                }
            } else {
                uploadResponse.errorBody()?.let { errorBody ->
                    try {
                        val error = Gson().fromJson(errorBody.string(),
                                                    StravaUpload400Error::class.java)
                        progressCallback(StravaUploadProgress.UploadFailure(name, error))
                        Log.i(TAG, error.toString())

                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing upload error response")
                        e.printStackTrace()
                        return
                    }
                }
            }
        }

        private fun copyToGz(inputStream: InputStream, outFile: File): File {
            val os = GZIPOutputStream(outFile.outputStream())
            inputStream.copyTo(os)
            os.close()
            return outFile
        }

        private suspend fun checkProgress(service: StravaService,
                                          progressCallback: (StravaUploadProgress) -> Unit,
                                          upload: StravaUpload) {

            val start = SystemClock.elapsedRealtime()
            var delayMs = StravaConfig.UPLOAD_PROGRESS_CHECK_INCREMENT_MS
            var count = 0

            while (true) {
                count++

                val uploadFromCheck = service.getUpload(upload.id).await()
                Log.i(TAG, "uploadResult check #$count: $uploadFromCheck")
                if (uploadFromCheck.activityId != null) {
                    progressCallback(StravaUploadProgress.UploadProcessingCompleted(
                            upload.activityId.toString(),
                            upload))
                    return
                }

                val elapsedTimeMs = SystemClock.elapsedRealtime() - start
                if (elapsedTimeMs > StravaConfig.UPLOAD_PROGRESS_CHECK_MAX_DURATION_S * 1000L) {
                    Log.e(TAG, "Maximum upload chedk duration reached")
                    progressCallback(StravaUploadProgress.UploadProcessingTimeout(
                            upload.activityId.toString(),
                            upload))
                    return
                }

                delay(delayMs)
                delayMs += StravaConfig.UPLOAD_PROGRESS_CHECK_INCREMENT_MS
            }
        }
    }
}

sealed class StravaUploadProgress(val name: String) {
    class Preparing(name: String) :
            StravaUploadProgress(name)

    class Uploaded(name: String, val upload: StravaUpload) :
            StravaUploadProgress(name)

    class UploadFailure(name: String, val upload400error: StravaUpload400Error) :
            StravaUploadProgress(name)

    class UploadProcessingCompleted(name: String, val upload: StravaUpload) :
            StravaUploadProgress(name)

    class UploadProcessingTimeout(name: String, val upload: StravaUpload) :
            StravaUploadProgress(name)
}

data class StravaUploadError(
        @SerializedName("errors") val errors: StravaUploadSingleError,
        @SerializedName("message") val message: String
)

data class StravaUploadSingleError(
        @SerializedName("code") val code: String,
        @SerializedName("field") val field: String,
        @SerializedName("resource") val resource: String
)

data class StravaUpload400Error(
        @SerializedName("id") val id: Long,
        @SerializedName("external_id") val externalId: String,
        @SerializedName("error") val error: String,
        @SerializedName("status") val status: String,
        @SerializedName("activity_id") val activityId: Long?
)

enum class UploadDataType(private val value: String) {
    FIT("fit"),
    FIT_GZ("fit.gz"),
    TCX("tcx"),
    TCX_GZ("tcx.gz"),
    GPX("gpx"),
    GPX_GZ("gpx.gz");

    companion object {
        fun fromFileExtension(fileName: String, compressUncompressed: Boolean): UploadDataType? {

            val nameLc = fileName.toLowerCase()
            val withCompressPref = if (compressUncompressed) "$nameLc.gz" else nameLc

            return when {
                withCompressPref.endsWith("gpx") -> GPX
                withCompressPref.endsWith("tcx") -> TCX
                withCompressPref.endsWith("fit") -> FIT
                withCompressPref.endsWith("gpx.gz") -> GPX_GZ
                withCompressPref.endsWith("tcx.gz") -> TCX_GZ
                withCompressPref.endsWith("fit.gz") -> FIT_GZ
                else -> null
            }
        }
    }

    override fun toString(): String {
        return value
    }
}
