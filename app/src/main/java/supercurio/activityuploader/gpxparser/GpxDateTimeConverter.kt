package supercurio.activityuploader.gpxparser

import android.util.Log
import com.tickaroo.tikxml.TypeConverter
import java.text.SimpleDateFormat
import java.util.*

class GpxDateTimeConverter : TypeConverter<Date?> {

    private val formatters = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    )

    override fun read(value: String?): Date? = parseDate(value)

    override fun write(value: Date?): String = formatters[1].format(value)

    private fun parseDate(value: String?): Date? {
        formatters.forEach {
            try {
                return it.parse(value)
            } catch (e: Exception) {
                if (it == formatters.last())
                    Log.e(TAG, "Error parsing \"$value\": ${e.message}")
            }
        }
        return null
    }

    companion object {
        private const val TAG = "GpxDateTimeConverter"
    }
}
