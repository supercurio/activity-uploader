package supercurio.activityuploader.fitbitwebapi

import android.content.Context
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Deferred
import java.util.*

data class FitbitStepsData(

        @SerializedName("activities-steps") val activitiesSteps: List<Steps>,
        @SerializedName("activities-steps-intraday") val activitiesStepsIntraday: StepsDataSet

) {

    companion object {

        fun getStepsData(context: Context,
                         startDateTime: Date,
                         endDateTime: Date,
                         offsetFromUTCMillis: Long,
                         detailLevel: FitbitDetailLevel): Deferred<FitbitStepsData> {

            if (detailLevel == FitbitDetailLevel.SECOND)
                throw IllegalArgumentException("Invalid detail level")

            val service = Intraday.getService(context)
            val intraday = Intraday(startDateTime, endDateTime, offsetFromUTCMillis)

            return service.getStepsData(intraday.startDate, intraday.startTime,
                                        intraday.endDate, intraday.endTime,
                                        detailLevel)
        }
    }
}

data class Steps(
        @SerializedName("dateTime") val dateTime: Date,
        @SerializedName("value") val value: Long
)

data class StepsDataSet(
        @SerializedName("dataset") val dataset: List<FitbitEntryPoint>
)
