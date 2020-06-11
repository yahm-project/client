package it.unibo.yahm.client.entities

import com.google.android.gms.maps.model.LatLng
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Representation of geographical coordinate.
 */
data class Coordinate(
    val latitude: Double,
    val longitude: Double
) {

    fun toLatLng() = LatLng(latitude, longitude)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Coordinate

        if (latitude.hash() != other.latitude.hash()) return false
        if (longitude.hash() != other.longitude.hash()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hash()
        result = 31 * result + longitude.hash()
        return result
    }

    private fun Double.hash(): Int {
        return (this * 10f.pow(6)).roundToInt()
    }

}
