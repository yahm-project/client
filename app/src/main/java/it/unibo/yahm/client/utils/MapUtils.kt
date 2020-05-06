package it.unibo.yahm.client.utils

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

const val EARTH_RADIUS = 3958.75
const val METER_CONVERSION = 1609
class MapUtils {

    companion object {
        fun getSurroundingMarkers(
            markers: List<Marker>?,
            origin: LatLng, maxDistanceMeters: Int,
            radius: Int
        ): List<Marker> {
            val surroundingMarkers = Collections.emptyList<Marker>()
            if (markers == null) return surroundingMarkers
            for (marker in markers) {
                val dist = distBetween(origin, marker.position).toDouble()
                if (dist < radius) {
                    surroundingMarkers.add(marker)
                }
            }
            return surroundingMarkers
        }

        private fun distBetween(pos1: LatLng, pos2: LatLng): Float {
            return distBetween(
                pos1.latitude, pos1.longitude, pos2.latitude,
                pos2.longitude
            )
        }

        /** distance in meters  */
        private fun distBetween(
            lat1: Double,
            lng1: Double,
            lat2: Double,
            lng2: Double
        ): Float {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = (sin(dLat / 2) * sin(dLat / 2)
                    + (cos(Math.toRadians(lat1))
                    * cos(Math.toRadians(lat2)) * sin(dLng / 2)
                    * sin(dLng / 2)))
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val dist = EARTH_RADIUS * c

            return (dist * METER_CONVERSION).toFloat()
        }
    }
}