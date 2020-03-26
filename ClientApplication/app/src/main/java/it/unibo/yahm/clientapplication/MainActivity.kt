package it.unibo.yahm.clientapplication

import android.content.Context
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.subjects.PublishSubject

class MainActivity : AppCompatActivity() {

    var reactiveSensors: ReactiveSensors? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        reactiveSensors = ReactiveSensors(sensorManager)

        readFromSensor()


    }

    private fun readFromSensor() {
        val accelerometer = reactiveSensors!!.observerFor("ACCELEROMETER")
        val gyroscope = reactiveSensors!!.observerFor("GYROSCOPE")

        val combined = Observables.combineLatest(accelerometer, gyroscope) { a, b ->
            "${a.sensor.stringType} ${a.values.toList()} -- ${b.sensor.stringType} ${b.values.toList()}"

        }.subscribe {
            Log.i("YAHM", it)
        }


    }


}
