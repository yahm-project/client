package it.unibo.yahm.clientapplication

import android.content.Context
import android.graphics.Color
import android.graphics.Interpolator
import android.graphics.Point
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import io.reactivex.rxjava3.kotlin.Observables
import java.util.Collections
import kotlin.concurrent.thread
import kotlin.random.Random


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val DELTA_ROTATION: Float = 30f
    private lateinit var mMap: GoogleMap
    private var carLocation: LatLng? = null
    private var carRotation: Float = 0f
    private var carMarker: Marker? = null
    private var drawedPolylines: List<Polyline> = Collections.emptyList()
    private  var reactiveSensors: ReactiveSensors? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        reactiveSensors = ReactiveSensors(sensorManager)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        carLocation = LatLng(44.22, 12.05)
        carMarker = mMap.addMarker(
            MarkerOptions()
                .position(carLocation!!)
                .rotation(carRotation)
        )


        /*
        var testPos = Pair(44.22, 12.06)
        var testRotation = 90f
        val handler = Handler()
        val handlerCode = object : Runnable {
            override fun run() {
                updateCarLocation(testPos)
                updateCarRotation(testRotation)
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLng(
                        LatLng(
                            testPos.first,
                            testPos.second
                        )
                    )
                )
                testPos = Pair(testPos.first, testPos.second + 0.01)
                handler.postDelayed(this, 5000)
            }
        }
        mMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraUpdate),
            object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    handler.post(handlerCode)
                }

                override fun onCancel() {

                }
            })
         */
        observeCarSensors()
    }

    fun drawRoadSegment(coordinates: List<Pair<Double, Double>>, argbColor: Int) {
        val polyline = mMap.addPolyline(
            PolylineOptions().addAll(coordinates.map { LatLng(it.first, it.second) })
                .color(argbColor)
        )
        drawedPolylines += polyline
    }

    fun updateCarLocation(location: Pair<Double, Double>) {
        val newCoordinates = LatLng(location.first, location.second)
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val proj: Projection = mMap.projection
        val startPoint: Point = proj.toScreenLocation(carMarker!!.position)
        val startLatLng = proj.fromScreenLocation(startPoint)
        val duration: Long = 500

        val interpolator = LinearInterpolator()

        val runnableCode = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val lng: Double = t * newCoordinates.longitude + (1 - t) * startLatLng.longitude
                val lat: Double = t * newCoordinates.latitude + (1 - t) * startLatLng.latitude
                carMarker!!.setPosition(LatLng(lat, lng))
                if (t < 1.0) { // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        }
        handler.post(runnableCode)
    }

    /*
        La mappa deve adattare il proprio orientamento facendo in modo che il marker sia sempre rivolto verso l'alto,
        questo richiede che la mappa e la camera ruotino in sincronia non appena la rotazione dell'auto superi un certo delta rispetto ad essi.
    */
    fun updatePerspective(targetRotation: Float) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val currentRotation = carMarker!!.rotation
        val duration: Long = 500
        val interpolator = LinearInterpolator()

        val rotateCar = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val newRotation: Float = t * targetRotation + (1 - t) * currentRotation
                carMarker!!.rotation = newRotation
                if (t < 1.0) { // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        }
        val rotateCamera = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val newRotation: Float = t * targetRotation + (1 - t) * currentRotation

                val cameraUpdate = CameraPosition.Builder()
                    .target(carLocation)
                    .zoom(15f)
                    .bearing(newRotation)
                    .tilt(45f)
                    .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraUpdate))
                if (t < 1.0) { // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        }
        if(Math.abs(mMap.cameraPosition.bearing - carMarker!!.rotation) < DELTA_ROTATION ) {
            handler.post(rotateCar)
        } else {
            handler.post(rotateCamera)
        }

    }

    private fun observeCarSensors() {
        val gps = reactiveSensors!!.observerFor("FINE_LOCATION")
        val gyroscope = reactiveSensors!!.observerFor("GYROSCOPE")

        gps.subscribe {
            updateCarLocation(Pair(it.values[0].toDouble(), it.values[1].toDouble()))
        }

        gyroscope.subscribe {
            updatePerspective(it.values[0])
        }
    }
}
