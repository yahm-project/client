package it.unibo.yahm.client.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.*
import io.reactivex.schedulers.Schedulers
import it.unibo.yahm.BuildConfig
import it.unibo.yahm.R
import it.unibo.yahm.client.SpotholeService
import it.unibo.yahm.client.entities.Evaluations
import it.unibo.yahm.client.sensors.*
import it.unibo.yahm.client.training.FakeQualityClassifier
import it.unibo.yahm.client.utils.CsvFile
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.system.exitProcess


class SimulationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulation)
        checkPermissions()
        initSavingStretches()
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

        startSavingStretches.setOnClickListener {
            csvFile.open()
            Log.i("SimulationActivity", "Saving to ${csvFile.fileName!!}")

            val sensorObservers = SensorObservers(
                ReactiveLocation(applicationContext),
                ReactiveSensor(applicationContext)
            )
            FakeQualityClassifier.process(sensorObservers.observeForStretchQualityProcessing())
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
            csvFile.close()
        }
    }

    // TODO: wip
    @SuppressLint("CheckResult")
    private fun testServer() {
        val baseUrl = if (BuildConfig.DEBUG) {
            applicationContext.resources.getString(R.string.spothole_service_development_baseurl)
        } else {
            applicationContext.resources.getString(R.string.spothole_service_production_baseurl)
        }

        val retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        val service = retrofit.create(SpotholeService::class.java)
        service.sendEvaluations(
            Evaluations(
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList()
            )
        ).subscribeOn(Schedulers.newThread())
            .subscribe({ a -> Log.i("aaaa", "ok") }, { e -> Log.i("aaa", e.toString()) })


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
