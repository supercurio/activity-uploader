package supercurio.activityuploader.fitbitwebapi

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

class Intraday(startDateTime: Date, endDateTime: Date, offsetFromUTCMillis: Long) {

    val startDate: String
    val startTime: String
    val endDate: String
    val endTime: String

    init {
        val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US)
        val timeFormatter = SimpleDateFormat(TIME_FORMAT, Locale.US)

        val start = startDateTime.offset(Calendar.MILLISECOND, offsetFromUTCMillis)
        val end = endDateTime.offset(Calendar.MILLISECOND, offsetFromUTCMillis)

        startDate = dateFormatter.format(start)
        startTime = timeFormatter.format(start)
        endDate = dateFormatter.format(end)
        endTime = timeFormatter.format(end)
    }

    companion object {
        const val DATE_FORMAT = "yyyy-MM-dd"
        const val TIME_FORMAT = "HH:mm:ss"

        fun getService(context: Context): FitbitService {
            val gson = GsonBuilder()
                    .setDateFormat(DATE_FORMAT)
                    .create()

            return FitbitWebApi.getService(context, gson)
        }
    }
}

data class FitbitEntryPoint(
        @SerializedName("time") val time: String,
        @SerializedName("value") val value: Double
)

enum class FitbitDetailLevel(val value: String) {
    MINUTE("1min"),
    SECOND("1sec");

    override fun toString(): String {
        return value
    }
}

fun String.toDateTime(activityStartDate: Date, offsetFromUtcMillis: Long): Date {
    val (hour, minute, second) = this
            .split(':')
            .map { v -> v.toInt() }

    val activityDateTimeCalendar = Calendar.getInstance()
    activityDateTimeCalendar.time = activityStartDate
    activityDateTimeCalendar.set(Calendar.HOUR, hour)
    activityDateTimeCalendar.set(Calendar.MINUTE, minute)
    activityDateTimeCalendar.set(Calendar.SECOND, second)
    activityDateTimeCalendar.set(Calendar.MILLISECOND, 0)
    activityDateTimeCalendar.add(Calendar.MILLISECOND, -offsetFromUtcMillis.toInt())

    return activityDateTimeCalendar.time
}

fun Date.offset(field: Int, amount: Long): Date {
    val cal = Calendar.getInstance()
    cal.time = this
    cal.add(field, amount.toInt())
    return cal.time
}
