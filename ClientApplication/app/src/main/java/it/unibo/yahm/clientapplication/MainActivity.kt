package it.unibo.yahm.clientapplication
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.util.Log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.subjects.PublishSubject

class MainActivity : AppCompatActivity() {

    private var reactiveSensors: ReactiveSensors? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnShowMap = findViewById<Button>(R.id.btnShowMap)

        btnShowMap.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
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
