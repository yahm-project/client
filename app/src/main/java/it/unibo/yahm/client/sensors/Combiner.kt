package it.unibo.yahm.client.sensors

import io.reactivex.rxjava3.core.Observable

class Combiner {

    companion object {
        fun create(): Combiner = Combiner()
    }

    private fun combinedSensorDataFactory(sensorsValues: List<SensorData>): CombinedSensorsData {
        val combinedDataBuilder = CombinedSensorsDataBuilder()
        sensorsValues.forEach {
            when (it) {
                is AccelerationData -> combinedDataBuilder.xAcceleration(it.xAcceleration)
                    .yAcceleration(it.yAcceleration).zAcceleration(it.zAcceleration)
                is GyroscopeData -> combinedDataBuilder.xAngularVelocity(it.xAngularVelocity)
                    .yAngularVelocity(it.yAngularVelocity).zAngularVelocity(it.zAngularVelocity)
                is GpsData -> combinedDataBuilder.latitude(it.latitude).longitude(it.longitude)
                    .speed(it.speed)
            }
        }
        return combinedDataBuilder.build()
    }

    fun combine(vararg sensorStreams: Observable<SensorData>): Observable<CombinedSensorsData> {
        return Observable.create { emitter ->
            val latestValues = arrayOfNulls<SensorData>(sensorStreams.size)

            sensorStreams.forEachIndexed { index, observable ->
                observable.subscribe { sensorData ->
                    latestValues[index] = sensorData
                    if (latestValues.all { it != null }) {
                        emitter.onNext(combinedSensorDataFactory(latestValues.map { it!! }))
                    }
                }
            }
        }
    }
}
