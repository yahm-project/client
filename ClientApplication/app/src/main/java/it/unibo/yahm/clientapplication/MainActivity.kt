package it.unibo.yahm.clientapplication

import android.content.Context
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.subjects.PublishSubject

class MainActivity : AppCompatActivity() {

    var reactiveSensors: ReactiveSensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        reactiveSensors = ReactiveSensor(sensorManager)

        readFromSensor()


    }

    private fun readFromSensor() {
        val accelerometerData = reactiveSensors!!.observerFor("ACCELEROMETER")
        val gyroscopeData = reactiveSensors!!.observerFor("GYROSCOPE")
        val gpsData = reactiveSensors!!.observeGPS(applicationContext)
        if (gpsData != null) {
            val combinedData = Observables.combineLatest(accelerometerData, gyroscopeData, gpsData) { a, b, c ->
                "${a.sensor.stringType} ${a.values.toList()} -- ${b.sensor.stringType} ${b.values.toList()} -- GPS ${c.latitude}, ${c.longitude}, ${c.speed}"

            }.subscribe {
                Log.i("YAHM", it)
            }
        }
    }
}
