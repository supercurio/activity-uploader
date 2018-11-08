package supercurio.activityuploader.gpxparser

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.annotation.*
import okio.Okio
import org.gavaghan.geodesy.Ellipsoid
import org.gavaghan.geodesy.GeodeticCalculator
import org.gavaghan.geodesy.GlobalCoordinates
import org.gavaghan.geodesy.GlobalPosition
import java.io.File
import java.util.*

@Xml
data class Gpx(

        @Attribute val creator: String?,
        @Attribute val version: String?,
        @Attribute(name = "xsi:schemaLocation") val schemaLocation: String?,
        @Element val metadata: Metadata?,
        @Element val tracks: List<Track>

) {
    fun applyTimeOffset(field: Int, amount: Int) {
        if (amount == 0) return

        metadata?.time = metadata?.time?.let {
            val cal = Calendar.getInstance()
            cal.time = it
            cal.add(field, amount)
            cal.time
        }

        tracks.forEach { track ->
            track.segments.forEach { segment ->
                segment.points.forEach { point ->
                    val cal = Calendar.getInstance()
                    cal.time = point.time
                    cal.add(field, amount)
                    point.time = cal.time
                }
            }
        }
    }

    companion object {
        fun read(gpxFile: File, exceptionOnUnreadXml: Boolean = true): Gpx {
            val parser = TikXml.Builder()
                    .exceptionOnUnreadXml(exceptionOnUnreadXml)
                    .build()

            return parser.read(Okio.buffer(Okio.source(gpxFile)), Gpx::class.java)
        }

        fun read(gpxFile: String, exceptionOnUnreadXml: Boolean = true) =
                read(File(gpxFile), exceptionOnUnreadXml)
    }
}

@Xml
data class Metadata(
        @PropertyElement(converter = GpxDateTimeConverter::class) var time: Date?,
        @PropertyElement val name: String?,
        @Element val author: Author?,
        @Element val link: Link?
)

@Xml
data class Author(
        @PropertyElement val name: String?
)

@Xml
data class Link(
        @Attribute val href: String?,
        @PropertyElement val text: String?
)

@Xml(name = "trk")
data class Track(
        @PropertyElement val name: String?,
        @PropertyElement val type: String?,
        @Element val segments: List<Segment>
)

@Xml(name = "trkseg")
data class Segment(
        @Element val points: List<Point>
)

@Xml(name = "trkpt")
data class Point(
        @Attribute(name = "lat") val latitude: Double,
        @Attribute(name = "lon") val longitude: Double,
        @PropertyElement(name = "ele") val elevation: Double?,
        @PropertyElement(converter = GpxDateTimeConverter::class) var time: Date,

        @Path("extensions/gpxtpx:TrackPointExtension")
        @PropertyElement(name = "gpxtpx:hr") val hrGpxtpx: Int?,
        @Path("extensions/ns3:TrackPointExtension")
        @PropertyElement(name = "ns3:hr") val hrNs3: Int?,

        @Path("extensions/gpxtpx:TrackPointExtension")
        @PropertyElement(name = "gpxtpx:cad") val cadGpxtpx: Int?,
        @Path("extensions/ns3:TrackPointExtension")
        @PropertyElement(name = "ns3:cad") val cadNs3: Int?
) {
    fun globalPosition() =
            GlobalPosition(
                    globalCoordinates(),
                    elevation ?: 0.0)

    fun globalCoordinates() = GlobalCoordinates(latitude,
                                                longitude)

    fun distanceMeters(otherPoint: Point,
                       geoCalc: GeodeticCalculator): Double {

        val geodeticCurve = if (elevation != null)
            geoCalc.calculateGeodeticMeasurement(
                    Ellipsoid.WGS84,
                    globalPosition(),
                    otherPoint.globalPosition()
            )
        else
            geoCalc.calculateGeodeticCurve(
                    Ellipsoid.WGS84,
                    globalCoordinates(),
                    otherPoint.globalCoordinates()
            )

        return geodeticCurve.ellipsoidalDistance
    }

}

fun Point.hr(): Int? {
    return hrGpxtpx ?: hrNs3
}

fun Gpx.startDate(): Date? = tracks.first()
        .segments.first()
        .points.first()
        .time

fun Gpx.endDate(): Date? = tracks.last()
        .segments.last()
        .points.last()
        .time
