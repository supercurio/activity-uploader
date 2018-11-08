package supercurio.activityuploader.fitbitwebapi

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.util.*

data class FitbitDevice(

        @SerializedName("battery") val battery: String,
        @SerializedName("batteryLevel") val batteryLevel: Short,
        @SerializedName("deviceVersion") val deviceVersion: String,
        @SerializedName("id") val id: Long,
        @SerializedName("lastSyncTime") val lastSyncTime: Date,
        @SerializedName("mac") val mac: String,
        @SerializedName("type") val type: DeviceType

) {
    companion object {
        fun getService(context: Context): FitbitService {
            val gson = GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss")
                    .create()

            return FitbitWebApi.getService(context, gson)
        }
    }
}

enum class DeviceType(val value: String) {
    @SerializedName("TRACKER")
    TRACKER("TRACKER"),

    @SerializedName("WATCH")
    WATCH("WATCH"),
}
