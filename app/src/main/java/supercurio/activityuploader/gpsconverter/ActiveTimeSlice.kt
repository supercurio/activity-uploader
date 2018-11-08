package supercurio.activityuploader.gpsconverter

import supercurio.activityuploader.gpxparser.Point

class ActiveTimeSlice {

    val points = mutableListOf<Point>()

    fun durationS(): Long? {
        return (points.last().time.time - points.first().time.time) / 1000
    }

    fun durationMinutes(): Double {
        val elapsedMs = points.last().time.time - points.first().time.time
        return elapsedMs.toDouble() / 1000 / 60.0
    }
}
