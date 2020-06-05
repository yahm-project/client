package it.unibo.yahm.client.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import it.unibo.yahm.R


class MainActivity : AppCompatActivity() {

    private var showMapButton: Button? = null
    private var showTrainingButton: Button? = null
    private var showSimulationButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))

        showMapButton = findViewById<Button>(R.id.showMap)
        showMapButton?.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        showTrainingButton = findViewById<Button>(R.id.showTraining)
        showTrainingButton?.setOnClickListener {
            val intent = Intent(this, TrainingActivity::class.java)
            startActivity(intent)
        }

        showSimulationButton = findViewById<Button>(R.id.showSimulation)
        showSimulationButton?.setOnClickListener {
            val intent = Intent(this, SimulationActivity::class.java)
            startActivity(intent)
        }

        checkPermissions()
    }


    private fun checkPermissions() {
        val requestPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                for (permission in result.keys) {
                    when (permission) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            // If request is cancelled, the result map is empty.
                            if (result.isNotEmpty() && !result[permission]!!) {
                                Log.d("PERMISSION", "ACCESS_FINE_LOCATION denied.")
                                showMapButton?.isEnabled = false
                                showTrainingButton?.isEnabled = false
                            }
                        }
                        Manifest.permission.INTERNET -> {
                            // If request is cancelled, the result map is empty.
                            if (result.isNotEmpty() && !result[permission]!!) {
                                Log.d("PERMISSION", "INTERNET denied.")
                                showMapButton?.isEnabled = false
                            }
                        }
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                            // If request is cancelled, the result map is empty.
                            if (result.isNotEmpty() && !result[permission]!!) {
                                Log.d("PERMISSION", "EXTERNAL_STORAGE denied.")
                                showTrainingButton?.isEnabled = false
                            }
                        }
                    }
                }
            }

        requestPermissions.launch(
            arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }

}
