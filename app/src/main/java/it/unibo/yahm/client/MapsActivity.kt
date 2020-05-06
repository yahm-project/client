package it.unibo.yahm.client

import it.unibo.yahm.R
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import it.unibo.yahm.client.utils.DrawableUtils
import it.unibo.yahm.client.sensors.CompassData
import it.unibo.yahm.client.sensors.GpsData
import it.unibo.yahm.client.sensors.ReactiveSensor
import it.unibo.yahm.client.sensors.SensorType
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private val DEFAULT_LOCATION = LatLng(45.133331, 12.233333)
        private const val DELTA_TRIGGER_ROTATION: Int = 15
        private const val DELTA_CAMERA_ROTATION: Int = 45
        private const val ZOOM: Float = 17f
        private const val TILT: Float = 45f
        private const val BEARING: Float = 0f
    }

    private lateinit var mMap: GoogleMap
    private var carMarker: Marker? = null
    private var drawedRoadSegmentStatus: List<Polyline> = Collections.emptyList()
    private var drawedRoadIssues: List<Marker> = Collections.emptyList()
    private var reactiveSensors: ReactiveSensor? = null
    private var currentCameraBearing = BEARING


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
        carMarker = addCarMarker(DEFAULT_LOCATION, BEARING)
        observeCarSensors()
    }

    fun drawRoadSegmentStatus(coordinates: List<Pair<Double, Double>>, argbColor: Int) {
        runOnUiThread {
            val polyline = mMap.addPolyline(
                PolylineOptions().addAll(coordinates.map { LatLng(it.first, it.second) })
                    .color(argbColor)
            )
            drawedRoadSegmentStatus += polyline
        }
    }

    fun drawRoadIssueOnPoint(coordinate: Pair<Double, Double>, issue: RoadIssue) {
        val drawable = when (issue) {
            RoadIssue.POT_HOLE -> R.drawable.ic_up_arrow_circle
            RoadIssue.ROAD_DRAIN -> R.drawable.ic_up_arrow_circle
            RoadIssue.SPEED_BUMP -> R.drawable.ic_up_arrow_circle
            RoadIssue.ROAD_JOINT -> R.drawable.ic_up_arrow_circle

        }
        val circleDrawable = ContextCompat.getDrawable(applicationContext, drawable)
        val markerIcon = DrawableUtils.getMarkerIconFromDrawable(circleDrawable!!)

        runOnUiThread {
            drawedRoadIssues += mMap.addMarker(
                MarkerOptions()
                    .position(LatLng(coordinate.first, coordinate.second))
                    .anchor(0.5f, 0.5f)
                    .icon(markerIcon)
            )
        }
    }

    private fun updateCarLocation(newCoordinates: LatLng) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val projection: Projection = mMap.projection
        val startPoint: Point = projection.toScreenLocation(carMarker!!.position)
        val startLatLng = projection.fromScreenLocation(startPoint)
        val duration: Long = 500
        val interpolator = LinearInterpolator()

        val runnableCode = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)

                if(t > 1.0 ) {
                    updateCameraLocation(location = newCoordinates, bearing = currentCameraBearing)
                    runOnUiThread { carMarker!!.position = LatLng(newCoordinates.latitude, newCoordinates.longitude) }
                } else {
                    val lng: Double = t * newCoordinates.longitude + (1 - t) * startLatLng.longitude
                    val lat: Double = t * newCoordinates.latitude + (1 - t) * startLatLng.latitude
                    runOnUiThread { carMarker!!.position = LatLng(lat, lng) }
                    handler.postDelayed(this, 16)
                }
            }
        }
        handler.post(runnableCode)

    }

    private fun updateCameraLocation(
        location: LatLng,
        zoom: Float = ZOOM,
        tilt: Float = TILT,
        bearing: Float = BEARING
    ) {
        val cameraUpdate = CameraPosition.Builder()
            .target(location)
            .zoom(zoom)
            .tilt(tilt)
            .bearing(bearing)
            .build();
        currentCameraBearing = bearing
        runOnUiThread { mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraUpdate)) }
    }

    private fun fixCameraPerspective() {
        val mapCenter: LatLng = mMap.cameraPosition.target
        val projection: Projection = mMap.projection
        val centerPoint = projection.toScreenLocation(mapCenter)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics) //not needed?
        val displayHeight = displayMetrics.heightPixels
        centerPoint.y =
            centerPoint.y - (displayHeight / 4.5).toInt() // move center down for approx 22%
        val newCenterPoint = projection.fromScreenLocation(centerPoint)
        runOnUiThread {
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    newCenterPoint,
                    ZOOM
                )
            )
        }
    }

    private fun rotationGap(first: Float, second: Float): Float {
        val rotationDelta: Float = (abs(first - second) % 360.0f)
        return if (rotationDelta > 180.0f) (360.0f - rotationDelta) else rotationDelta
    }

    /*
        La mappa deve adattare il proprio orientamento facendo in modo che il marker sia sempre rivolto verso l'alto,
        questo richiede che la mappa e la camera ruotino in sincronia non appena la rotazione dell'auto superi un certo delta rispetto ad essi.
    */
    private var isRotating = false
    private var currentCarTargetRotation = BEARING
    private val duration: Long = 500
    private val carRotationHandler = Handler()

    private fun updateRotation(newTargetRotation: Float) {
        if (rotationGap(currentCarTargetRotation, newTargetRotation) > DELTA_TRIGGER_ROTATION && !isRotating) {
            currentCarTargetRotation = newTargetRotation
            val start = SystemClock.uptimeMillis()
            val currentRotation = carMarker!!.rotation
            val interpolator = LinearInterpolator()

            val rotateCar = object : Runnable {
                override fun run() {
                    isRotating = true
                    val elapsed = SystemClock.uptimeMillis() - start
                    val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                    val newRotation: Float = t * currentCarTargetRotation + (1 - t) * currentRotation

                    if (t < 1.0) { // Post again 16ms later.
                        runOnUiThread { carMarker!!.rotation = newRotation }
                        carRotationHandler.postDelayed(this, 16)
                    } else {
                        isRotating = false
                    }
                }
            }

            if (rotationGap(mMap.cameraPosition.bearing, newTargetRotation) < DELTA_CAMERA_ROTATION) {
                Log.d("MapActivity", "Rotating only the car")
                carRotationHandler.post(rotateCar)
            } else {
                Log.d("MapActivity", "Rotating everything")
                carRotationHandler.post(rotateCar)
                updateCameraLocation(location = carMarker!!.position, bearing = newTargetRotation)
            }
        }
    }

    private fun observeCarSensors() {
        val gps = reactiveSensors!!.observerFor(SensorType.GPS)
        val compass = reactiveSensors!!.observerFor(SensorType.ROTATION_VECTOR)

        gps.observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val coordinates = LatLng((it as GpsData).latitude, it.longitude)
                Log.d("MapActivity", "GPS Update: $coordinates")
                updateCarLocation(coordinates)
            }
        compass.observeOn(AndroidSchedulers.mainThread()).subscribe {
            val orientation = (it as CompassData).orientation
            //Log.d("MapActivity", "$orientation")
            updateRotation(orientation);
        }

        /*
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
        handler.post(handlerCode)*/
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
