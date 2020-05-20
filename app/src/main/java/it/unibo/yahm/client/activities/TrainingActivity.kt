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
import it.unibo.yahm.client.utils.FunctionUtils.stdDeviation
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
                "timestamp",
                "x_min_acc", "y_min_acc", "z_min_acc",
                "x_max_acc", "y_max_acc", "z_max_acc",
                "x_avg_acc", "y_avg_acc", "z_avg_acc",
                "x_med_acc", "y_med_acc", "z_med_acc",
                "x_sdv_acc", "y_sdv_acc", "z_sdv_acc",
                "x_min_ang_vel", "y_min_ang_vel", "z_min_ang_vel",
                "x_max_ang_vel", "y_max_ang_vel", "z_max_ang_vel",
                "x_avg_ang_vel", "y_avg_ang_vel", "z_avg_ang_vel",
                "x_med_ang_vel", "y_med_ang_vel", "z_med_ang_vel",
                "x_sdv_ang_vel", "y_sdv_ang_vel", "z_sdv_ang_vel",
                "latitude", "longitude", "speed"
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
                        cv.accelerationValues.map { it.x }.min(),
                        cv.accelerationValues.map { it.y }.min(),
                        cv.accelerationValues.map { it.z }.min(),
                        cv.accelerationValues.map { it.x }.max(),
                        cv.accelerationValues.map { it.y }.max(),
                        cv.accelerationValues.map { it.z }.max(),
                        cv.accelerationValues.map { it.x }.average(),
                        cv.accelerationValues.map { it.y }.average(),
                        cv.accelerationValues.map { it.z }.average(),
                        cv.accelerationValues.map { it.x }.median(),
                        cv.accelerationValues.map { it.y }.median(),
                        cv.accelerationValues.map { it.z }.median(),
                        cv.accelerationValues.map { it.x }.stdDeviation(),
                        cv.accelerationValues.map { it.y }.stdDeviation(),
                        cv.accelerationValues.map { it.z }.stdDeviation(),
                        cv.gyroscopeValues.map { it.x }.min(),
                        cv.gyroscopeValues.map { it.y }.min(),
                        cv.gyroscopeValues.map { it.z }.min(),
                        cv.gyroscopeValues.map { it.x }.max(),
                        cv.gyroscopeValues.map { it.y }.max(),
                        cv.gyroscopeValues.map { it.z }.max(),
                        cv.gyroscopeValues.map { it.x }.average(),
                        cv.gyroscopeValues.map { it.y }.average(),
                        cv.gyroscopeValues.map { it.z }.average(),
                        cv.gyroscopeValues.map { it.x }.median(),
                        cv.gyroscopeValues.map { it.y }.median(),
                        cv.gyroscopeValues.map { it.z }.median(),
                        cv.gyroscopeValues.map { it.x }.stdDeviation(),
                        cv.gyroscopeValues.map { it.y }.stdDeviation(),
                        cv.gyroscopeValues.map { it.z }.stdDeviation(),
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
            // reactiveSensor.dispose(SensorType.LINEAR_ACCELERATION)
            // reactiveSensor.dispose(SensorType.GYROSCOPE)
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
