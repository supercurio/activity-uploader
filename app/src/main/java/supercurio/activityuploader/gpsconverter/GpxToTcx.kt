package supercurio.activityuploader.gpsconverter

import android.content.Context
import android.util.Log
import com.sweetzpot.tcxzpot.*
import com.sweetzpot.tcxzpot.Track
import com.sweetzpot.tcxzpot.builders.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.gavaghan.geodesy.GeodeticCalculator
import supercurio.activityuploader.R
import supercurio.activityuploader.fitbitwebapi.*
import supercurio.activityuploader.gpxparser.*
import java.io.File
import java.util.*
import kotlin.math.roundToInt

class GpxToTcx(val context: Context, private val inputGpx: String) {

    private val converterJob = Job()
    private val bgScope = CoroutineScope(Dispatchers.IO + converterJob)

    fun convert(outputTcx: String,
                timeOffsetType: Int = Calendar.HOUR,
                timeOffsetAmount: Int = 0,
                fitbitOffsetFromUtcMillis: Long) {

        bgScope.launch {
            val gpx = Gpx.read(File(inputGpx))
            gpx.applyTimeOffset(timeOffsetType, timeOffsetAmount)

            val activityStartDate = gpx.startDate()?.offset(Calendar.MINUTE, -1)
            val activityEndDate = gpx.endDate()?.offset(Calendar.MINUTE, 1)

            if (activityStartDate == null ||
                    activityEndDate == null ||
                    activityStartDate == activityEndDate) {
                Log.e(TAG, "Invalid GPX dates")
                return@launch
            }

            val activeTimeSlices = findActiveTimeSlices(gpx)

            val hrInterpolator = getFitbitHrInterpolator(
                    context,
                    activityStartDate,
                    activityEndDate,
                    fitbitOffsetFromUtcMillis)

            val caloriesInterpolator = getFitbitCaloriesInterpolator(
                    context,
                    activityStartDate,
                    activityEndDate,
                    fitbitOffsetFromUtcMillis)

            val stepsInterpolator = getFitbitFilteredStepsDataInterpolator(
                    context,
                    activityStartDate,
                    activityEndDate,
                    fitbitOffsetFromUtcMillis,
                    activeTimeSlices)

            val laps = getLaps(activeTimeSlices,
                               hrInterpolator,
                               caloriesInterpolator,
                               stepsInterpolator)

            saveToTcx(outputTcx, gpx, laps)
        }
    }

    private suspend fun getFitbitHrInterpolator(
            context: Context,
            activityStartDate: Date,
            activityEndDate: Date,
            fitbitOffsetFromUtcMillis: Long): FitbitEntryPointInterpolator? {

        val data = FitbitHrData.getHrData(context,
                                          activityStartDate,
                                          activityEndDate,
                                          fitbitOffsetFromUtcMillis,
                                          FitbitDetailLevel.SECOND)

        return FitbitEntryPointInterpolator(
                data.activitiesHeart.first().dateTime,
                fitbitOffsetFromUtcMillis,
                data.activitiesHeartIntraday.dataset)
    }

    private suspend fun getFitbitCaloriesInterpolator(
            context: Context,
            activityStartDate: Date,
            activityEndDate: Date,
            fitbitOffsetFromUtcMillis: Long): FitbitEntryPointInterpolator? {

        val data = FitbitCaloriesData.getCaloriesData(context,
                                                      activityStartDate,
                                                      activityEndDate,
                                                      fitbitOffsetFromUtcMillis,
                                                      FitbitDetailLevel.MINUTE)

        return FitbitEntryPointInterpolator(
                data.activitiesCalories.first().dateTime,
                fitbitOffsetFromUtcMillis,
                data.activitiesCaloriesIntraday.dataset)
    }

    private suspend fun getFitbitFilteredStepsDataInterpolator(
            context: Context,
            activityStartDate: Date,
            activityEndDate: Date,
            fitbitOffsetFromUtcMillis: Long,
            activeTimeSlices: List<ActiveTimeSlice>): FitbitEntryPointInterpolator? {

        val stepsData = FitbitStepsData.getStepsData(context,
                                                     activityStartDate,
                                                     activityEndDate,
                                                     fitbitOffsetFromUtcMillis,
                                                     FitbitDetailLevel.MINUTE).await()

        val stepsDataStartDate = stepsData.activitiesSteps.first().dateTime

        // remove minute entries from steps data set when there was a pause, since it will
        // make the step count for this minute invalid
        val filteredStepsDataSet = mutableListOf<FitbitEntryPoint>()
        activeTimeSlices.forEach { slice ->
            val sliceStartTime = slice.points.first().time.time
            val sliceEndTime = slice.points.last().time.time

            stepsData.activitiesStepsIntraday.dataset.forEach { entryPoint ->
                val pointStartTime = entryPoint.time
                        .toDateTime(stepsDataStartDate, fitbitOffsetFromUtcMillis)
                        .time

                val pointEndTime = pointStartTime + 60 * 1_000 // 60 seconds

                if (pointStartTime > sliceStartTime && pointEndTime < sliceEndTime)
                    filteredStepsDataSet += entryPoint
            }
        }

        return FitbitEntryPointInterpolator(
                stepsDataStartDate,
                fitbitOffsetFromUtcMillis,
                filteredStepsDataSet)
    }

    private fun getLaps(activeTimeSlices: List<ActiveTimeSlice>,
                        hrInterpolator: FitbitEntryPointInterpolator? = null,
                        caloriesInterpolator: FitbitEntryPointInterpolator? = null,
                        stepsInterpolator: FitbitEntryPointInterpolator? = null): List<Lap> {
        val geoCalc = GeodeticCalculator()
        val laps = mutableListOf<Lap>()
        activeTimeSlices.forEach { slice ->

            val tcxPoints = mutableListOf<Trackpoint>()

            var distanceMeters = 0.0
            var caloriesAcc = 0.0
            var stepsAcc = 0.0

            var previousPoint: Point? = null
            slice.points.forEach { point ->
                previousPoint?.let { prev ->
                    distanceMeters += prev.distanceMeters(point, geoCalc)
                }
                previousPoint = point

                val trackPoint = TrackpointBuilder
                        .aTrackpoint()
                        .onTime(TCXDate(point.time))
                        .withPosition(Position.position(point.latitude,
                                                        point.longitude))

                hrInterpolator?.getValue(point.time)?.let {
                    trackPoint.withHeartRate(HeartRate(it.roundToInt()))
                } ?: point.hr()?.let { it -> trackPoint.withHeartRate(HeartRate(it)) }

                caloriesInterpolator?.getValue(point.time)?.let {
                    caloriesAcc += it
                }

                stepsInterpolator?.getValue(point.time)?.let {
                    val rpm = it / 2.0
                    stepsAcc += rpm
                    trackPoint.withCadence(Cadence(rpm.roundToInt()))
                }

                point.elevation?.let { trackPoint.withAltitude(it) }

                tcxPoints += trackPoint.build()
            }

            val lapDurationSeconds = (slice.points.last().time.time -
                    slice.points.first().time.time).toDouble() / 1_000.0

            val lapBuilder = LapBuilder.aLap(TCXDate(slice.points.first().time))
                    .withTotalTime(lapDurationSeconds)
                    .withIntensity(Intensity.ACTIVE)
                    .withTriggerMethod(TriggerMethod.MANUAL)
                    .withTracks(Track.trackWith(tcxPoints))

            // calculate calories
            if (caloriesAcc > 0) {
                val calories = caloriesAcc / slice.points.size * slice.durationMinutes()
                lapBuilder.withCalories(calories.roundToInt())
            }

            // calculate cadence for this lap
            if (stepsAcc > 0) {
                val cadence = stepsAcc / slice.points.size / slice.durationMinutes()
                lapBuilder.withCadence(Cadence(cadence.roundToInt()))
            }

            laps += lapBuilder.build()
        }

        return laps
    }

    private fun saveToTcx(outputTcx: String,
                          gpx: Gpx,
                          laps: List<Lap>) {
        val serializer = FormattedXmlSerializer(File(outputTcx))

        TrainingCenterDatabaseBuilder.trainingCenterDatabase()
                .withActivities(Activities.activities(
                        ActivityBuilder.activity(Sport.RUNNING)
                                .withID(TCXDate(gpx.startDate()))
                                .withLaps(laps)))
                .withAuthor(
                        ApplicationBuilder.application(context.getString(R.string.app_name))
                                .withBuild(BuildBuilder.aBuild().withVersion(
                                        VersionBuilder.version()
                                                .major(0)
                                                .minor(1)))
                                .withLanguageID("en")
                                .withPartNumber("0")
                ).build()
                .serialize(serializer)
        serializer.save()
    }

    private fun findActiveTimeSlices(gpx: Gpx): List<ActiveTimeSlice> {
        val pauseMs = 2_000

        val timeSlices = mutableListOf<ActiveTimeSlice>()
        var timeSlice = ActiveTimeSlice()
        timeSlices += timeSlice

        val gpxPoints = gpx.tracks
                .flatMap { track -> track.segments }
                .flatMap { segment -> segment.points }


        gpxPoints.forEach { point ->
            val lastPointMs = timeSlice.points.lastOrNull()?.time?.time ?: point.time.time
            val currentPointMs = point.time.time

            if (currentPointMs > lastPointMs + pauseMs) {
                timeSlice = ActiveTimeSlice()
                timeSlices += timeSlice
            }

            timeSlice.points += point
        }

        return timeSlices
    }


    companion object {
        private const val TAG = "GpxToTcx"
    }
}
