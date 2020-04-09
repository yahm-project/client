package it.unibo.yahm.clientapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import io.reactivex.rxjava3.core.Observable
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    var reactiveSensor: ReactiveSensor? = null
    var gpsData: Observable<SensorData>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnShowMap = findViewById<Button>(R.id.btnShowMap)

        btnShowMap.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        reactiveSensor = ReactiveSensor(applicationContext)
        val accelerometerData = reactiveSensor!!.observerFor(SensorType.ACCELEROMETER)
        val gyroscopeData = reactiveSensor!!.observerFor(SensorType.GYROSCOPE)
        try {
            gpsData = reactiveSensor!!.observerFor(SensorType.GPS)
        } catch (illegalAccess: IllegalAccessError) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
        Combiner.create().combine(accelerometerData, gyroscopeData, gpsData!!).subscribe {
            Log.i("YAHM", it.toString())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            gpsData = reactiveSensor!!.observerFor(SensorType.GPS)
        } else {
            this.finish()
            exitProcess(1)
        }
    }
}
