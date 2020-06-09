package it.unibo.yahm.client.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import kotlin.math.*


class MapUtils {

    companion object {
        private const val EARTH_RADIUS = 3958.75
        private const val METER_CONVERSION = 1609

        /** distance in meters  */
        fun distBetween(pointA: LatLng, pointB: LatLng): Float {
            val dLat = Math.toRadians(pointB.latitude - pointA.latitude)
            val dLng = Math.toRadians(pointB.longitude - pointA.longitude)
            val a = (sin(dLat / 2) * sin(dLat / 2)
                    + (cos(Math.toRadians(pointA.latitude))
                    * cos(Math.toRadians(pointB.latitude)) * sin(dLng / 2)
                    * sin(dLng / 2)))
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val dist = EARTH_RADIUS * c

            return (dist * METER_CONVERSION).toFloat()
        }

        fun getVisibleRadius(visibleRegion: VisibleRegion): Float {
            val diagonalDistance = FloatArray(1)
            val farLeft = visibleRegion.farLeft;
            val nearRight = visibleRegion.nearRight;

            Location.distanceBetween(
                farLeft.latitude,
                farLeft.longitude,
                nearRight.latitude,
                nearRight.longitude,
                diagonalDistance
            )

            return diagonalDistance[0] / 2;
        }

        fun rotationGap(first: Float, second: Float): Float {
            val rotationDelta: Float = (abs(first - second) % 360.0f)
            return if (rotationDelta > 180.0f) (360.0f - rotationDelta) else rotationDelta
        }
    }

}
