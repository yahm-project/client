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

        checkPermissions()

        showMapButton = findViewById(R.id.showMap)
        showMapButton.visibility = View.GONE
        showMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        showTrainingButton = findViewById(R.id.showTraining)
        showTrainingButton.visibility = View.GONE
        showTrainingButton.setOnClickListener {
            val intent = Intent(this, TrainingActivity::class.java)
            startActivity(intent)
        }

        showSimulationButton = findViewById(R.id.showSimulation)
        showSimulationButton.visibility = View.GONE
        showSimulationButton.setOnClickListener {
            val intent = Intent(this, SimulationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        val requestPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                val permissions = result.filter { it.value }.map {it.key}
                val mapPermissionsGranted = permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)

                Log.d(javaClass.name, "Granted permissions: $permissions")

                if (BuildConfig.DEBUG) {
                    showMapButton.visibility = View.VISIBLE
                    showTrainingButton.visibility = View.VISIBLE
                    showSimulationButton.visibility = View.VISIBLE

                    showMapButton.isEnabled = mapPermissionsGranted
                    showTrainingButton.isEnabled = listOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).all { permissions.contains(it) }
                    showSimulationButton.isEnabled = listOf(Manifest.permission.ACCESS_FINE_LOCATION,
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

        val permissions = mutableListOf(Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION)
        if (BuildConfig.DEBUG) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        requestPermissions.launch(permissions.toTypedArray())
    }

}
