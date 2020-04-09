package it.unibo.yahm.clientapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.io.*
import java.text.DateFormat
import java.util.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private var reactiveSensor: ReactiveSensor? = null
    private var gpsData: Observable<SensorData>? = null
    private var disposable: Disposable? = null
    private var sensorDatasFile: FileUtils? = null
    private var userClicksFile: FileUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val startButton = findViewById<Button>(R.id.start)
        val stopButton = findViewById<Button>(R.id.stop)
        val spotHoleButton = findViewById<Button>(R.id.spotHole)
        val spotBackButton = findViewById<Button>(R.id.spotBack)
        val spotRoadJointButton = findViewById<Button>(R.id.spotRoadJoint)
        val spotManHoleButton = findViewById<Button>(R.id.spotManHole)
        stopButton.isEnabled = false
        spotHoleButton.isEnabled= false
        spotBackButton.isEnabled= false
        spotManHoleButton.isEnabled= false
        spotRoadJointButton.isEnabled= false
        reactiveSensor = ReactiveSensor(applicationContext)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        startButton.setOnClickListener {
            val dateAndTime = DateFormat.getDateTimeInstance().format(Date())
            sensorDatasFile = createFile("${dateAndTime}_sensors.csv")
            userClicksFile = createFile("${dateAndTime}_userClicks.csv")
            writeOnFile(CSV_SENSOR_HEADER, file = sensorDatasFile!!)
            writeOnFile(CSV_USER_CLICK_HEADER, file = userClicksFile!!)
            val accelerometerData = reactiveSensor!!.observerFor(SensorType.ACCELEROMETER)
            val gyroscopeData = reactiveSensor!!.observerFor(SensorType.GYROSCOPE)
            gpsData = reactiveSensor!!.observerFor(SensorType.GPS)
            val combinedStream: Observable<CombinedSensorsData> = Combiner.create().combine(accelerometerData, gyroscopeData, gpsData!!)
            disposable = combinedStream.map {
                "${System.currentTimeMillis()},${it.xAcceleration},${it.yAcceleration},${it.zAcceleration}," +
                        "${it.xAngularVelocity},${it.yAngularVelocity},${it.zAngularVelocity}," +
                        "${it.latitude},${it.longitude},${it.speed}"
            }.buffer(BUFFERED_ELEMENT).subscribe {
                writeOnFile(toWrite = *it.toTypedArray(), file = sensorDatasFile!!)
            }
            stopButton.isEnabled = true
            spotHoleButton.isEnabled= true
            spotBackButton.isEnabled= true
            spotManHoleButton.isEnabled= true
            spotRoadJointButton.isEnabled= true
            startButton.isEnabled = false
            val spotButtonClickListener = View.OnClickListener {
                writeOnFile("${System.currentTimeMillis()},${(it as Button).text}", file = userClicksFile!!)
            }
            spotHoleButton.setOnClickListener(spotButtonClickListener)
            spotBackButton.setOnClickListener(spotButtonClickListener)
            spotManHoleButton.setOnClickListener(spotButtonClickListener)
            spotRoadJointButton.setOnClickListener(spotButtonClickListener)
        }

        stopButton.setOnClickListener {
            disposable!!.dispose()
            stopButton.isEnabled = false
            spotHoleButton.isEnabled= false
            spotBackButton.isEnabled= false
            spotManHoleButton.isEnabled= false
            spotRoadJointButton.isEnabled= false
            startButton.isEnabled = true
            sensorDatasFile!!.close()
            userClicksFile!!.close()
        }
    }

    private fun createFile(fileName: String): FileUtils {
        try {
            return FileUtils(fileName, applicationContext)
        } catch (fileNotFound: FileNotFoundException) {
            Toast.makeText(applicationContext, "Unable to find or create specified file!", Toast.LENGTH_LONG).show()
            exitProcess(1)
        }
    }

    private fun writeOnFile(vararg toWrite: String, file: FileUtils) {
        try {
            if (toWrite.size == 1) file.writeLine(toWrite[0]) else file.writeLines(toWrite.toList())
        } catch (exception: IOException) {
            Log.i("YAHM", "Unable to write on file. Cause: ${exception.message}")
            exitProcess(1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
       grantResults.forEach { Log.i("YAHM", it.toString()) }
        when(requestCode) {
            1 -> {
                if(grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED} ){
                    gpsData = reactiveSensor!!.observerFor(SensorType.GPS)
                } else {
                    this.finish()
                    exitProcess(1)
                }
            }
        }
    }

    companion object {
        private const val CSV_SENSOR_HEADER = "Timestamp,xAcc,yAcc,zAcc,xAngVel,yAngVel,zAngVel,lat,lon,speed"
        private const val BUFFERED_ELEMENT = 3
        private const val CSV_USER_CLICK_HEADER = "Timestamp,obj"
    }
}
