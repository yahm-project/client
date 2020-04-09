package it.unibo.yahm.clientapplication

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.reactivex.rxjava3.core.Observable


class ReactiveSensors(private val sensorManager: SensorManager) {

    private val listeners: MutableMap<Int, SensorEventListener> = HashMap()
    private val observers: MutableMap<Int, Observable<SensorEvent>> = HashMap()


    fun observerFor(sensorType: String): Observable<SensorEvent> {
        val sensorIntType = sensorIntType(sensorType)
        if (observers.containsKey(sensorIntType)) {
            return observers[sensorIntType]!!
        }

        val sensor = sensorManager.getDefaultSensor(sensorIntType)

        return Observable.create<SensorEvent> {
            val listener = object: SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Nope
                }

                override fun onSensorChanged(event: SensorEvent?) {
                    it.onNext(event)
                }
            }

            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            listeners[sensorIntType] = listener
        }
    }

    fun stopRead(sensorType: String) {
        val sensorIntType = sensorIntType(sensorType)
        if (!listeners.containsKey(sensorIntType)) {
            throw IllegalStateException("No listener for type $sensorType")
        }

        sensorManager.unregisterListener(listeners.remove(sensorIntType))
    }

    private fun sensorIntType(sensorType: String): Int = when (sensorType) {
        "ACCELEROMETER" -> Sensor.TYPE_ACCELEROMETER
        "ACCELEROMETER_UNCALIBRATED" -> Sensor.TYPE_ACCELEROMETER_UNCALIBRATED
        "GRAVITY" -> Sensor.TYPE_GRAVITY
        "GYROSCOPE" -> Sensor.TYPE_GYROSCOPE
        "GYROSCOPE_UNCALIBRATED" -> Sensor.TYPE_GYROSCOPE_UNCALIBRATED
        "LINEAR_ACCELERATION" -> Sensor.TYPE_LINEAR_ACCELERATION
        "ROTATION_VECTOR" -> Sensor.TYPE_ROTATION_VECTOR
        "STEP_COUNTER" -> Sensor.TYPE_STEP_COUNTER
        else -> throw IllegalArgumentException("Invalid type: $sensorType")
    }
}
