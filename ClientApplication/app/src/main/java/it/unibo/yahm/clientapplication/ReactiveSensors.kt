package it.unibo.yahm.clientapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import io.reactivex.rxjava3.core.Observable
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import io.reactivex.rxjava3.subjects.PublishSubject


class ReactiveSensor(private val sensorManager: SensorManager) {

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
    fun observeGPS(context: Context): Observable<Location>? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val latestLocation: PublishSubject<Location> = PublishSubject.create();
        val locationListener = object: LocationListener {
            override fun onLocationChanged(location: Location?) {
                Log.i("YAHM", location.toString())
                latestLocation.onNext(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.v("YAHM_GPS", "Not yet implemented")
            }

            override fun onProviderEnabled(provider: String?) {
                Log.v("YAHM_GPS", "Not yet implemented")
            }

            override fun onProviderDisabled(provider: String?) {
                Log.v("YAHM_GPS", "Not yet implemented")
            }

        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100L, 0f, locationListener)
        return latestLocation;
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
