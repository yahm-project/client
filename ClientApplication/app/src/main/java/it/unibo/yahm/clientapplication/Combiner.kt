package it.unibo.yahm.clientapplication
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.combineLatest

class Combiner {

    companion object {
        fun create(): Combiner = Combiner()
    }

    private fun combinedSensorDataFactory(streams: List<SensorData>): CombinedSensorsData {
        val combinedDataBuilder = CombinedSensorsDataBuilder()
        streams.forEach{
            when(it) {
                is AccelerationData -> combinedDataBuilder.xAcceleration(it.xAcceleration).yAcceleration(it.yAcceleration).zAcceleration(it.zAcceleration)
                is GyroscopeData -> combinedDataBuilder.xAngularVelocity(it.xAngularVelocity).yAngularVelocity(it.yAngularVelocity).zAngularVelocity(it.zAngularVelocity)
                is GpsData -> combinedDataBuilder.latitude(it.latitude).longitude(it.longitude).speed(it.speed)
            }
        }
        return combinedDataBuilder.build()
    }

    fun combine(vararg sensorStreams: Observable<SensorData>): Observable<CombinedSensorsData> {
        return sensorStreams.asList().combineLatest { streams: List<SensorData> -> combinedSensorDataFactory(streams)}
    }
}
