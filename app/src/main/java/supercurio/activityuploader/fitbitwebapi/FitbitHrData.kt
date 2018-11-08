package supercurio.activityuploader.fitbitwebapi

import android.content.Context
import com.google.gson.annotations.SerializedName
import java.util.*

data class FitbitHrData(

        @SerializedName("activities-heart") val activitiesHeart: List<HrActivityHeart>,
        @SerializedName("activities-heart-intraday") val activitiesHeartIntraday: HrDataSet

) {
    companion object {
        suspend fun getHrData(context: Context,
                              startDateTime: Date,
                              endDateTime: Date,
                              offsetFromUTCMillis: Long,
                              detailLevel: FitbitDetailLevel): FitbitHrData {

            val service = Intraday.getService(context)
            val intraday = Intraday(startDateTime, endDateTime, offsetFromUTCMillis)

            return service.getHrData(intraday.startDate, intraday.startTime,
                                     intraday.endDate, intraday.endTime,
                                     detailLevel).await()
        }
    }
}

data class HrActivityHeart(
        @SerializedName("dateTime") val dateTime: Date,
        @SerializedName("heartRateZones") val heartRateZones: List<HeartRateZone>,
        @SerializedName("value") val value: Double
)

data class HeartRateZone(
        @SerializedName("caloriesOut") val caloriesOut: Double,
        @SerializedName("max") val max: Long,
        @SerializedName("min") val min: Long,
        @SerializedName("minutes") val minutes: Long,
        @SerializedName("name") val name: String
)

data class HrDataSet(
        @SerializedName("dataset") val dataset: List<FitbitEntryPoint>
)
