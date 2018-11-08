package supercurio.activityuploader.fitbitwebapi

import android.content.Context
import com.google.gson.annotations.SerializedName
import java.util.*

data class FitbitCaloriesData(

        @SerializedName("activities-calories")
        val activitiesCalories: List<FitbitCalories>,
        @SerializedName("activities-calories-intraday")
        val activitiesCaloriesIntraday: FitbitCaloriesDataSet

) {
    companion object {
        suspend fun getCaloriesData(context: Context,
                                    startDateTime: Date,
                                    endDateTime: Date,
                                    offsetFromUTCMillis: Long,
                                    detailLevel: FitbitDetailLevel): FitbitCaloriesData {

            if (detailLevel == FitbitDetailLevel.SECOND)
                throw IllegalArgumentException("Invalid detail level")

            val service = Intraday.getService(context)
            val intraday = Intraday(startDateTime, endDateTime, offsetFromUTCMillis)

            return service.getCaloriesData(intraday.startDate, intraday.startTime,
                                           intraday.endDate, intraday.endTime,
                                           detailLevel).await()
        }
    }
}

data class FitbitCalories(
        @SerializedName("dateTime") val dateTime: Date,
        @SerializedName("value") val value: Double
)

data class FitbitCaloriesDataSet(
        @SerializedName("dataset") val dataset: List<FitbitEntryPoint>
)
