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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))

        val showMapButton = findViewById<Button>(R.id.showMap)
        showMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        val showTrainingButton = findViewById<Button>(R.id.showTraining)
        showTrainingButton.setOnClickListener {
            val intent = Intent(this, TrainingActivity::class.java)
            startActivity(intent)
        }

        val btnSimulation = findViewById<Button>(R.id.btnSimulation)
        btnSimulation.setOnClickListener {
            val intent = Intent(this, SimulationActivity::class.java)
            startActivity(intent)
        }


        val requestPermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            Log.d("PERMISSION", result.toString())
            for (permission in result.keys) {
                when (permission) {
                    Manifest.permission.ACCESS_FINE_LOCATION -> {
                        // If request is cancelled, the result map is empty.
                        if (result.isNotEmpty() && result[permission]!!) {
                            Log.d("PERMISSION", "User granted permission")
                        } else {
                            Log.d("PERMISSION", "User denied permission")
                            showMapButton.isEnabled = false
                        }
                    }
                    Manifest.permission.INTERNET -> {
                        // If request is cancelled, the result map is empty.
                        if (result.isNotEmpty() && result[permission]!!) {
                            Log.d("PERMISSION", "User granted permission")
                        } else {
                            Log.d("PERMISSION", "User denied permission")
                            showMapButton.isEnabled = false
                        }
                    }
                    Manifest.permission.READ_EXTERNAL_STORAGE -> {
                        // If request is cancelled, the result map is empty.
                        if (result.isNotEmpty() && result[permission]!!) {
                            Log.d("PERMISSION", "User granted permission")
                        } else {
                            Log.d("PERMISSION", "User denied permission")
                            showTrainingButton.isEnabled = false
                        }
                    }
                    Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                        // If request is cancelled, the result map is empty.
                        if (result.isNotEmpty() && result[permission]!!) {
                            Log.d("PERMISSION", "User granted permission")
                        } else {
                            Log.d("PERMISSION", "User denied permission")
                            showTrainingButton.isEnabled = false
                        }
                    }
                    else -> {
                    }
                }
            }
        }

        when {
            /*ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_DENIED -> {
                Log.d("PERMISSION", "1")
                showMapButton.isEnabled = false
            }
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_DENIED -> {
                Log.d("PERMISSION", "2")
                showMapButton.isEnabled = false
            }
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED -> {
                Log.d("PERMISSION", "3")
                showTrainingButton.isEnabled = false
            }
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED -> {
                Log.d("PERMISSION", "4")
                showTrainingButton.isEnabled = false
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    applicationContext,
                    "Fine location is needed to track your movements.",
                    Toast.LENGTH_LONG
                ).show()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.INTERNET) -> {
                Toast.makeText(
                    applicationContext,
                    "Internet is needed to retrieve road issue data" +
                            " and inform other user of potential problem on the road you are driving on.",
                    Toast.LENGTH_LONG
                ).show()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                Toast.makeText(
                    applicationContext,
                    "Access to external storage is needed for training!",
                    Toast.LENGTH_LONG
                ).show()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                Toast.makeText(
                    applicationContext,
                    "Access to external storage is needed for training!",
                    Toast.LENGTH_LONG
                ).show()
            }*/
            else -> {
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }


        }
    }

}
