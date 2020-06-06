package it.unibo.yahm.client.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import it.unibo.yahm.R
import it.unibo.yahm.client.classifiers.FakeQualityClassifier
import it.unibo.yahm.client.entities.Evaluations
import it.unibo.yahm.client.sensors.ReactiveLocation
import it.unibo.yahm.client.sensors.ReactiveSensor
import it.unibo.yahm.client.sensors.SensorCombiners
import it.unibo.yahm.client.sensors.SensorType
import it.unibo.yahm.client.services.RetrofitService
import it.unibo.yahm.client.services.RoadClassifiersService
import it.unibo.yahm.client.utils.CsvFile


class SimulationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulation)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        initSavingStretches()
        initSendingStretches()
        initTestClassifier()
    }

    private fun initSavingStretches() {
        val startSavingStretches = findViewById<Button>(R.id.btnStartSavingStretches)
        val stopSavingStretches = findViewById<Button>(R.id.btnStopSavingStretches)

        val csvFile = CsvFile(
            "stretches", listOf(
                "latitude", "longitude", "radius",
                "timestamp", "quality"
            ), applicationContext
        )

        val reactiveLocation = ReactiveLocation(applicationContext)
        val reactiveSensor = ReactiveSensor(applicationContext)
        val sensorCombiners = SensorCombiners(reactiveLocation, reactiveSensor)

        startSavingStretches.setOnClickListener {
            startSavingStretches.isEnabled = false
            stopSavingStretches.isEnabled = true

            csvFile.open()
            Log.i("SimulationActivity", "Saving to ${csvFile.fileName!!}")

            sensorCombiners.combineByStretchLength().map(FakeQualityClassifier())
                .subscribe({
                    csvFile.writeValue(
                        listOf(
                            it.position.latitude, it.position.longitude, it.radius,
                            it.timestamp, it.quality
                        )
                    )
                }, { it.printStackTrace() })
        }

        stopSavingStretches.setOnClickListener {
            startSavingStretches.isEnabled = true
            stopSavingStretches.isEnabled = false

            reactiveLocation.dispose()
            reactiveSensor.dispose(SensorType.ACCELEROMETER)
            reactiveSensor.dispose(SensorType.GYROSCOPE)
            csvFile.close()
        }
    }

    private fun initSendingStretches() {
        val startSendingStretches = findViewById<Button>(R.id.btnStartSendingStretches)
        val stopSendingStretches = findViewById<Button>(R.id.btnStopSendingStretches)
        val bufferSize = 20

        val reactiveLocation = ReactiveLocation(applicationContext)
        val reactiveSensor = ReactiveSensor(applicationContext)
        val sensorCombiners = SensorCombiners(reactiveLocation, reactiveSensor)
        val service = RetrofitService(applicationContext).spotholeService

        startSendingStretches.setOnClickListener { _ ->
            startSendingStretches.isEnabled = false
            stopSendingStretches.isEnabled = true

            sensorCombiners.combineByStretchLength().map(FakeQualityClassifier())
                .buffer(bufferSize, bufferSize - 1)
                .flatMap { buf ->
                    service.sendEvaluations(
                        Evaluations(
                            buf.map { it.position },
                            buf.map { it.timestamp },
                            buf.map { it.radius },
                            buf.take(bufferSize - 1).map { it.quality },
                            emptyList()
                        )
                    )
                }.subscribe({
                    Log.i("SimulationActivity", "Make request to the server..")
                }, {
                    it.printStackTrace()
                })
        }

        stopSendingStretches.setOnClickListener {
            startSendingStretches.isEnabled = true
            stopSendingStretches.isEnabled = false

            reactiveLocation.dispose()
            reactiveSensor.dispose(SensorType.ACCELEROMETER)
            reactiveSensor.dispose(SensorType.GYROSCOPE)
        }
    }

    private fun initTestClassifier() {
        val testClassifier = findViewById<Button>(R.id.btnTestClassifier)

        val reactiveLocation = ReactiveLocation(applicationContext)
        val reactiveSensor = ReactiveSensor(applicationContext)
        val service = RetrofitService(applicationContext).spotholeService

        testClassifier.setOnClickListener {
            RoadClassifiersService(
                applicationContext, reactiveSensor, reactiveLocation, service
            ).startService()
        }
    }

}
