package it.unibo.yahm.clientapplication

import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import it.unibo.yahm.clientapplication.Utilities.DrawableUtils
import java.util.*
import kotlin.math.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private val DEFAULT_LOCATION = LatLng(44.133331, 12.233333)
        private const val DELTA_ROTATION: Float = 30f
        private const val ZOOM: Float = 17f
        private const val TILT: Float = 45f
        private const val BEARING: Float = 0f
    }

    private lateinit var mMap: GoogleMap
    private var carMarker: Marker? = null
    private var drawedPolylines: List<Polyline> = Collections.emptyList()
    private var reactiveSensors: ReactiveSensor? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        reactiveSensors = ReactiveSensor(applicationContext)
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
        carMarker = addCarMarker(DEFAULT_LOCATION!!, BEARING)
        updateCameraLocation(carMarker!!.position)
        observeCarSensors()
    }

    fun drawRoadSegment(coordinates: List<Pair<Double, Double>>, argbColor: Int) {
        val polyline = mMap.addPolyline(
            PolylineOptions().addAll(coordinates.map { LatLng(it.first, it.second) })
                .color(argbColor)
        )
        drawedPolylines += polyline
    }

    private fun updateCarLocation(location: Pair<Double, Double>) {
        val handler = Handler()
        val newCoordinates = LatLng(location.first, location.second)
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
                carMarker!!.position = LatLng(lat, lng)

                if (t < 1.0) { // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        }
        handler.post(runnableCode)
        updateCameraLocation(LatLng(location.first, location.second))
        //fixCameraPerspective()
    }

    private fun updateCameraLocation(location: LatLng, zoom: Float = ZOOM, tilt: Float = TILT, bearing: Float = BEARING) {
        val cameraUpdate = CameraPosition.Builder()
            .target(location)
            .zoom(zoom)
            .tilt(tilt)
            .bearing(bearing)
            .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraUpdate))
    }

    private fun fixCameraPerspective() {
        val mapCenter: LatLng = mMap.cameraPosition.target
        val projection: Projection = mMap.projection
        val centerPoint = projection.toScreenLocation(mapCenter)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics) //not needed?
        val displayHeight = displayMetrics.heightPixels
        centerPoint.y = centerPoint.y - (displayHeight / 4.5).toInt() // move center down for approx 22%
        val newCenterPoint = projection.fromScreenLocation(centerPoint)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newCenterPoint, ZOOM))
    }

    /*
        La mappa deve adattare il proprio orientamento facendo in modo che il marker sia sempre rivolto verso l'alto,
        questo richiede che la mappa e la camera ruotino in sincronia non appena la rotazione dell'auto superi un certo delta rispetto ad essi.
    */
    private fun updateRotation(targetRotation: Float) {
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
                    .target(carMarker!!.position)
                    .zoom(ZOOM)
                    .bearing(newRotation)
                    .tilt(TILT)
                    .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraUpdate))
                if (t < 1.0) { // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        }
        if (abs(mMap.cameraPosition.bearing - carMarker!!.rotation) < DELTA_ROTATION) {
            handler.post(rotateCar)
        } else {
            handler.post(rotateCamera)
        }

    }

    private fun observeCarSensors() {
        val gps = reactiveSensors!!.observerFor(SensorType.GPS)
        val gyroscope = reactiveSensors!!.observerFor(SensorType.GYROSCOPE)

        gps.subscribe {
            Log.d("MapActivity", "GPS Update: ${{(it as GpsData).latitude; it.longitude }}")
            updateCarLocation(Pair((it as GpsData).latitude, it.longitude))
        }

        //TODO: remove the following, just for debug
        val handler = Handler()
        var testPos = Pair(carMarker!!.position.latitude, carMarker!!.position.longitude)
        val handlerCode = object : Runnable {
            override fun run() {
                updateCarLocation(testPos)
                testPos = Pair(testPos.first + 0.001, testPos.second + 0.001)
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(handlerCode)

        /*
        gyroscope.subscribe {
            Log.d("MapActivity", "${{ (it as GyroscopeData).xAngularVelocity; it.yAngularVelocity; it.zAngularVelocity }}")
            updateRotation(0f); //TODO change it
        }*/

    }

    private fun addCarMarker(latLng: LatLng, rotation: Float): Marker {
        val circleDrawable =
            ContextCompat.getDrawable(applicationContext, R.drawable.ic_up_arrow_circle)
        val markerIcon = DrawableUtils.getMarkerIconFromDrawable(circleDrawable!!)

        return mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .anchor(0.5f, 0.5f)
                .rotation(rotation)
                .flat(true)
                .icon(markerIcon)
        )
    }



    /* Get segment's bearing the user is navigating */
    private fun getBearingForSegment(begin: LatLng, end: LatLng): Float {
        val dLon = end.longitude - begin.longitude
        val x = sin(Math.toRadians(dLon)) * cos(Math.toRadians(end.latitude))
        val y = (cos(Math.toRadians(begin.latitude)) * sin(Math.toRadians(end.latitude))
                - sin(Math.toRadians(begin.latitude)) * cos(Math.toRadians(end.latitude))
                * cos(Math.toRadians(dLon)))
        val bearing = Math.toDegrees(atan2(x, y))
        return bearing.toFloat()
    }
}
