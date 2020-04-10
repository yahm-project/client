package it.unibo.yahm.rxsensor

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
import com.google.android.gms.location.LocationServices
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.*
import kotlin.collections.HashMap


class ReactiveSensor(private val context: Context) {

    private val listeners: MutableMap<Int, SensorEventListener> = HashMap()
    private val observables = EnumMap<SensorType, Observable<SensorData>>(SensorType::class.java)
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private fun getObservableIfPresentOrExecuteAction(sensorType: SensorType, action: () -> Observable<SensorData>): Observable<SensorData> {
        return if (observables.containsKey(sensorType)) {
            observables[sensorType]!!
        } else {
            action()
        }
    }

    private fun observeMotionSensor(sensorType: SensorType): Observable<SensorData> {
        val sensorIntType = sensorIntType(sensorType.toString())
        val action: () -> Observable<SensorData> = {
            val sensor = sensorManager.getDefaultSensor(sensorIntType)

            val observableToReturn: Observable<SensorData> = Observable.create {
                val listener = object : SensorEventListener {
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        // Nope
                    }

                    override fun onSensorChanged(event: SensorEvent?) {
                        it.onNext(getSensorDataBasedOnSensorType(sensorType.toString(), event!!.values.toTypedArray()))
                    }
                }
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                listeners[sensorIntType] = listener
            }
            observables[sensorType] = observableToReturn
            observableToReturn
        }
        return getObservableIfPresentOrExecuteAction(sensorType, action)
    }

    private fun observeGPS(sensorType: SensorType): Observable<SensorData> {
        val action: () -> Observable<SensorData> = {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val latestLocation: PublishSubject<SensorData> = PublishSubject.create()
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    Log.i("YAHM_GPS", location.toString())
                    latestLocation.onNext(getSensorDataBasedOnSensorType(sensorType.toString(), arrayOf(location!!.latitude, location.longitude, location.speed)))
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
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                throw IllegalAccessError()
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 100f, locationListener)

            // TODO: should be moved?
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latestLocation.onNext(getSensorDataBasedOnSensorType(sensorType.toString(), arrayOf(location.latitude, location.longitude, location.speed)))
                }
            }

            observables[sensorType] = latestLocation
            latestLocation
        }
        return getObservableIfPresentOrExecuteAction(sensorType, action)
    }

    private fun observeRotationVector(sensorType: SensorType): Observable<SensorData> {
        val sensorIntType = sensorIntType(sensorType.toString())
        val action: () -> Observable<SensorData> = {
            val sensor = sensorManager.getDefaultSensor(sensorIntType)

            val observableToReturn: Observable<SensorData> = Observable.create {
                val listener = object : SensorEventListener {
                    val orientation =  FloatArray(3)
                    val rMat =  FloatArray(9)

                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int ) {}

                    override fun onSensorChanged( event: SensorEvent ) {
                        if( event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ){
                            // calculate th rotation matrix
                            SensorManager.getRotationMatrixFromVector( rMat, event.values );
                            // get the azimuth value (orientation[0]) in degree
                            val azimuth: Int = ((Math.toDegrees(SensorManager.getOrientation( rMat, orientation )[0].toDouble()) + 360 ) % 360).toInt()
                            it.onNext(getSensorDataBasedOnSensorType(sensorType.toString(), arrayOf(azimuth)))
                        }
                    }
                }
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                listeners[sensorIntType] = listener
            }
            observables[sensorType] = observableToReturn
            observableToReturn
        }
        return getObservableIfPresentOrExecuteAction(sensorType, action)
    }


    fun observerFor(sensorType: SensorType): Observable<SensorData> {
        return when (sensorType) {
            SensorType.ACCELEROMETER, SensorType.GYROSCOPE -> {
                observeMotionSensor(sensorType)
            }
            SensorType.GPS -> {
                observeGPS(sensorType)
            }
            SensorType.ROTATION_VECTOR ->
            {
                observeRotationVector(sensorType)
            }
        }
    }

    fun stopRead(sensorType: String) {
        val sensorIntType: Int = sensorIntType(sensorType)
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

    private fun getSensorDataBasedOnSensorType(sensorType: String, values: Array<out Number>): SensorData {
        return when (sensorType) {
            "ACCELEROMETER" -> {
                AccelerationData(xAcceleration = values[0].toFloat(), yAcceleration = values[1].toFloat(), zAcceleration = values[2].toFloat())
            }
            "GYROSCOPE" -> {
                GyroscopeData(xAngularVelocity = values[0].toFloat(), yAngularVelocity = values[1].toFloat(), zAngularVelocity = values[2].toFloat())
            }
            "GPS" -> {
                GpsData(latitude = values[0].toDouble(), longitude = values[1].toDouble(), speed = values[2].toFloat())
            }
            "ROTATION_VECTOR" -> {
                CompassData(orientation = values[0].toInt())
            }
            else -> throw IllegalArgumentException("Invalid type: $sensorType")
        }
    }
}
