package it.unibo.yahm.client.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.*


class ReactiveSensor(context: Context) {

    private val listeners = EnumMap<SensorType, SensorEventListener>(SensorType::class.java)
    private val publishSubjects =
        EnumMap<SensorType, PublishSubject<SensorEvent>>(SensorType::class.java)
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    @Synchronized
    fun observer(sensorType: SensorType): Observable<SensorEvent> =
        if (publishSubjects.containsKey(sensorType)) {
            publishSubjects[sensorType]!!
        } else {
            val observable = createObserver(sensorType)
            publishSubjects[sensorType] = observable
            observable
        }

    private fun createObserver(sensorType: SensorType): PublishSubject<SensorEvent> {
        val sensorIntType = sensorIntType(sensorType.toString())
        val sensor = sensorManager.getDefaultSensor(sensorIntType)

        val subject = PublishSubject.create<SensorEvent>()
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Nope
            }

            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    subject.onNext(event)
                }
            }
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        listeners[sensorType] = listener

        return subject
    }

    fun dispose(sensorType: SensorType) {
        if (!listeners.containsKey(sensorType)) {
            throw IllegalStateException("No listener for type $sensorType")
        }

        sensorManager.unregisterListener(listeners.remove(sensorType))
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
