package it.unibo.yahm.client.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import it.unibo.yahm.BuildConfig
import it.unibo.yahm.R
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var showMapButton: Button
    private lateinit var showTrainingButton: Button
    private lateinit var showSimulationButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))

        showMapButton = findViewById(R.id.showMap)
        showMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        showTrainingButton = findViewById(R.id.showTraining)
        showTrainingButton.setOnClickListener {
            val intent = Intent(this, TrainingActivity::class.java)
            startActivity(intent)
        }

        showSimulationButton = findViewById(R.id.showSimulation)
        showSimulationButton.setOnClickListener {
            val intent = Intent(this, SimulationActivity::class.java)
            startActivity(intent)
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val requestPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                val permissions = result.filter { it.value }.map {it.key}
                val mapPermissionsGranted = listOf(Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_FINE_LOCATION).all { permissions.contains(it) }

                if (BuildConfig.DEBUG) {
                    showMapButton.visibility = View.VISIBLE
                    showTrainingButton.visibility = View.VISIBLE
                    showSimulationButton.visibility = View.VISIBLE

                    showMapButton.isEnabled = mapPermissionsGranted
                    showTrainingButton.isEnabled = listOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .all { permissions.contains(it) }
                    showSimulationButton.isEnabled = listOf(Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).all { permissions.contains(it) }
                } else {
                    if (mapPermissionsGranted) {
                        val intent = Intent(this, MapsActivity::class.java)
                        startActivity(intent)
                    } else {
                        Log.w(javaClass.name, "Some required permissions are denied: ${result.filter { !it.value }}")
                        Toast.makeText(applicationContext, "Some required permissions are denied",
                            Toast.LENGTH_SHORT).show()
                        this.finish()
                        exitProcess(1)
                    }
                }
            }

        requestPermissions.launch(
            arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }

}
