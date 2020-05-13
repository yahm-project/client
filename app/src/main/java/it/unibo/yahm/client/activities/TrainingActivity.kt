package it.unibo.yahm.client.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import it.unibo.yahm.R
import it.unibo.yahm.client.sensors.ReactiveLocation
import it.unibo.yahm.client.sensors.ReactiveSensor
import it.unibo.yahm.client.sensors.SensorCombiners
import it.unibo.yahm.client.sensors.SensorType
import it.unibo.yahm.client.utils.CsvFile
import it.unibo.yahm.client.utils.FunctionUtils.median
import kotlin.system.exitProcess


class TrainingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        checkPermissions()
        initSavingEvents()
    }

    private fun initSavingEvents() {
        val startButton = findViewById<Button>(R.id.start)
        val stopButton = findViewById<Button>(R.id.stop)
        val spotHoleButton = findViewById<Button>(R.id.spotHole)
        val spotBackButton = findViewById<Button>(R.id.spotBack)
        val spotRoadJointButton = findViewById<Button>(R.id.spotRoadJoint)
        val spotManHoleButton = findViewById<Button>(R.id.spotManHole)
        stopButton.isEnabled = false
        spotHoleButton.isEnabled = false
        spotBackButton.isEnabled = false
        spotManHoleButton.isEnabled = false
        spotRoadJointButton.isEnabled = false

        val reactiveSensor = ReactiveSensor(applicationContext)
        val reactiveLocation = ReactiveLocation(applicationContext)
        val sensorObservers = SensorCombiners(reactiveLocation, reactiveSensor)

        checkPermissions()

        val sensorValuesFile = CsvFile(
            "sensor_values", listOf(
                "timestamp", "x_acc", "y_acc",
                "z_acc", "x_ang_vel", "y_ang_vel", "z_ang_vel", "latitude", "longitude", "speed"
            ),
            applicationContext
        )
        val obstaclesFile = CsvFile(
            "obstacles", listOf("timestamp", "type"),
            applicationContext
        )

        startButton.setOnClickListener { _ ->
            sensorValuesFile.open()
            obstaclesFile.open()
            Log.i("TrainingActivity", "Saving to ${sensorValuesFile.fileName}")
            Log.i("TrainingActivity", "Saving to ${obstaclesFile.fileName}")


            sensorObservers.combineByTime().subscribe({ cv ->
                sensorValuesFile.writeValue(
                    listOf(
                        cv.timestamp,
                        cv.accelerationValues.map { it.x }.median(),
                        cv.accelerationValues.map { it.y }.median(),
                        cv.accelerationValues.map { it.z }.median(),
                        cv.gyroscopeValues.map { it.x }.median(),
                        cv.gyroscopeValues.map { it.y }.median(),
                        cv.gyroscopeValues.map { it.z }.median(),
                        cv.location?.latitude,
                        cv.location?.longitude,
                        cv.location?.speed
                    )
                )
            }, { it.printStackTrace() })

            val spotButtonClickListener = View.OnClickListener {
                obstaclesFile.writeValue(listOf(System.currentTimeMillis(), (it as Button).text))
            }
            spotHoleButton.setOnClickListener(spotButtonClickListener)
            spotBackButton.setOnClickListener(spotButtonClickListener)
            spotManHoleButton.setOnClickListener(spotButtonClickListener)
            spotRoadJointButton.setOnClickListener(spotButtonClickListener)

            stopButton.isEnabled = true
            spotHoleButton.isEnabled = true
            spotBackButton.isEnabled = true
            spotManHoleButton.isEnabled = true
            spotRoadJointButton.isEnabled = true
            startButton.isEnabled = false
        }

        stopButton.setOnClickListener {
            stopButton.isEnabled = false
            spotHoleButton.isEnabled = false
            spotBackButton.isEnabled = false
            spotManHoleButton.isEnabled = false
            spotRoadJointButton.isEnabled = false
            startButton.isEnabled = true

            reactiveLocation.dispose()
            reactiveSensor.dispose(SensorType.ACCELEROMETER)
            reactiveSensor.dispose(SensorType.GYROSCOPE)
            sensorValuesFile.close()
            obstaclesFile.close()
        }
    }

    private fun checkPermissions() {
        requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                    this.finish()
                    exitProcess(1)
                }
            }
        }
    }

}
