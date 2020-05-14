package it.unibo.yahm.client.entities

import com.google.android.gms.maps.model.LatLng

data class Coordinate(
    val latitude: Double,
    val longitude: Double
) {

    fun toLatLng() = LatLng(latitude, longitude)
}
