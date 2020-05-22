package it.unibo.yahm.client.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import it.unibo.yahm.BuildConfig
import it.unibo.yahm.R
import it.unibo.yahm.client.SpotholeService
import it.unibo.yahm.client.entities.Evaluations
import it.unibo.yahm.client.sensors.ReactiveLocation
import it.unibo.yahm.client.sensors.ReactiveSensor
import it.unibo.yahm.client.sensors.SensorCombiners
import it.unibo.yahm.client.sensors.SensorType
import it.unibo.yahm.client.classifiers.FakeQualityClassifier
import it.unibo.yahm.client.utils.CsvFile
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.system.exitProcess


class SimulationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulation)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        checkPermissions()
        initSavingStretches()
        initSendingStretches()
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

        val baseUrl = if (BuildConfig.DEBUG) {
            applicationContext.resources.getString(R.string.spothole_service_development_baseurl)
        } else {
            applicationContext.resources.getString(R.string.spothole_service_production_baseurl)
        }

        val retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()

        val reactiveLocation = ReactiveLocation(applicationContext)
        val reactiveSensor = ReactiveSensor(applicationContext)
        val sensorCombiners = SensorCombiners(reactiveLocation, reactiveSensor)
        val service = retrofit.create(SpotholeService::class.java)

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

}
