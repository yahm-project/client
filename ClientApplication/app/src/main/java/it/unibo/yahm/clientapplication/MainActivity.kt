package it.unibo.yahm.clientapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.observers.DisposableObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    var reactiveSensor: ReactiveSensor? = null
    var gpsData: Observable<SensorData>? = null
    var lastEmittedData: CombinedSensorsData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        reactiveSensor = ReactiveSensor(applicationContext)
        val accelerometerData = reactiveSensor!!.observerFor(SensorType.ACCELEROMETER)
        val gyroscopeData = reactiveSensor!!.observerFor(SensorType.GYROSCOPE)
        try {
            gpsData = reactiveSensor!!.observerFor(SensorType.GPS)
        } catch (illegalAccess: IllegalAccessError) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        val combinedStream: Observable<CombinedSensorsData> = Combiner.create().combine(accelerometerData, gyroscopeData, gpsData!!)
        combinedStream.subscribe { lastEmittedData = it }
        findViewById<Button>(R.id.spotHole).setOnClickListener {
            Log.i("YAHM", lastEmittedData.toString())
        }

    }

    private fun getLastElement(stream: Observable<CombinedSensorsData>) {
        stream.lastElement().doOnEvent { value, error ->
            if (value == null && error == null) {
                Log.i("YAHM", "empty")
            } else {
                Log.i("YAHM", "value")
            }
        }.subscribe()
        stream.lastElement().subscribe({ Log.i("YAHM", it.toString()) }, {
            throw IllegalStateException()
        }, { Log.i("YAHM", "completed") })
        /*stream.
        last(CombinedSensorsData(-1f, -1f, -1f, -1f, -1f, -1f, -1.0, -1.0, -1f)).
        subscribe { success ->  Log.i("YAHM", success.toString())}*/
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            gpsData = reactiveSensor!!.observerFor(SensorType.GPS)
        } else {
            this.finish()
            exitProcess(1)
        }
    }

    /*  private fun readFromSensor() {
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
      }*/
}
