package it.unibo.yahm.client.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.*
import it.unibo.yahm.R
import it.unibo.yahm.client.sensors.*
import it.unibo.yahm.client.utils.CsvFile
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
        val sensorObservers = SensorObservers(reactiveLocation, reactiveSensor)

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

            sensorObservers.observeForSensorValues({ accelerations, angularVelocities, gpsLocation ->
                SensorValues(
                    Acceleration(
                        accelerations.map { it.x }.average(),
                        accelerations.map { it.y }.average(),
                        accelerations.map { it.z }.average()
                    ),
                    AngularVelocity(
                        angularVelocities.map { it.x }.average(),
                        angularVelocities.map { it.y }.average(),
                        angularVelocities.map { it.z }.average()
                    ),
                    gpsLocation
                )
            }, 100).subscribe({
                sensorValuesFile.writeValue(
                    listOf(
                        it.gpsLocation?.time, it.acceleration.x,
                        it.acceleration.y, it.acceleration.z, it.angularVelocity.x,
                        it.angularVelocity.y, it.angularVelocity.z, it.gpsLocation?.latitude,
                        it.gpsLocation?.longitude, it.gpsLocation?.speed
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
