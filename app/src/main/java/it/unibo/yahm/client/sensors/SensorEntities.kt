package it.unibo.yahm.client.sensors

import android.hardware.SensorEvent
import android.location.Location
import it.unibo.yahm.client.entities.Coordinate
import it.unibo.yahm.client.entities.Quality


data class Acceleration(
    val x: Double,
    val y: Double,
    val z: Double
) {
    companion object {
        fun fromSensorEvent(sensorEvent: SensorEvent): Acceleration =
            Acceleration(sensorEvent.values[0].toDouble(), sensorEvent.values[1].toDouble(),
                sensorEvent.values[2].toDouble())
    }
}

data class AngularVelocity(
    val x: Double,
    val y: Double,
    val z: Double
) {
    companion object {
        fun fromSensorEvent(sensorEvent: SensorEvent): AngularVelocity =
            AngularVelocity(sensorEvent.values[0].toDouble(), sensorEvent.values[1].toDouble(),
                sensorEvent.values[2].toDouble())
    }
}

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val speed: Float?,
    val time: Long
) {

    companion object {
        fun fromLocation(location: Location): GpsLocation = GpsLocation(
            location.latitude,
            location.longitude,
            if (location.hasAccuracy()) location.accuracy else null,
            if (location.hasSpeed()) location.speed else null,
            location.time
        )
    }
}

// TODO: rename this shit
data class StretchQualityInput(
    val location: Location,
    val length: Double,
    val accelerationEvents: List<SensorEvent>,
    val gyroscopeEvents: List<SensorEvent>
)

data class StretchQualityOutput(
    val position: Coordinate,
    val timestamp: Long,
    val radius: Double,
    val quality: Quality
)

data class SensorValues(
    val acceleration: Acceleration,
    val angularVelocity: AngularVelocity,
    val timestamp: Long,
    val gpsLocation: GpsLocation?
)
