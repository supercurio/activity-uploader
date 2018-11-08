package supercurio.activityuploader.gpsconverter

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import supercurio.activityuploader.fitbitwebapi.FitbitEntryPoint
import supercurio.activityuploader.fitbitwebapi.toDateTime
import java.util.*


/**
 * Created by François Simond on 10/9/18.
 * Part of fitbit-hr
 */
class FitbitEntryPointInterpolator(
        private val activityStartDate: Date,
        private val offsetFromUtcMillis: Long,
        dataset: List<FitbitEntryPoint>
) {

    private val function: PolynomialSplineFunction
    private val timeMin: Double
    private val timeMax: Double

    init {
        val time = dataset.map {
            it.time
                    .toDateTime(activityStartDate,
                                offsetFromUtcMillis)
                    .time
                    .toDouble()
        }.toDoubleArray()

        timeMin = time.first()
        timeMax = time.last()

        val values = dataset.map { it.value }.toDoubleArray()

        val interpolator = LinearInterpolator()
        function = interpolator.interpolate(time, values)
    }

    fun getValue(dateTime: Date): Double? {
        val time = dateTime.time.toDouble()
        val x = time.coerceIn(timeMin, timeMax)
        return try {
            function.value(x)
        } catch (e: Exception) {
            null
        }
    }
}
