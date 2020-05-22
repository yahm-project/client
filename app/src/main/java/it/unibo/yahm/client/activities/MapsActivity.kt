package it.unibo.yahm.client.activities

import android.Manifest
import android.content.res.ColorStateList
import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import it.unibo.yahm.BuildConfig
import it.unibo.yahm.client.SpotholeService
import it.unibo.yahm.client.utils.CustomTileProvider
import it.unibo.yahm.client.utils.DrawableUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.math.*
import it.unibo.yahm.R
import it.unibo.yahm.client.entities.*
import it.unibo.yahm.client.sensors.*
import it.unibo.yahm.client.utils.MapUtils

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private val DEFAULT_LOCATION = LatLng(45.133331, 12.233333)
        private const val DELTA_TRIGGER_ROTATION: Int = 15
        private const val DELTA_CAMERA_ROTATION: Int = 45
        private const val ZOOM: Float = 17f
        private const val TILT: Float = 45f
        private const val BEARING: Float = 0f
        private const val MAX_RADIUS_METERS = 2000f //won't ask for disease further away than that
        private const val DEFAULT_RADIUS_METERS = MAX_RADIUS_METERS / 10
        private const val BUFFER_SIZE = 20
    }

    private lateinit var mMap: GoogleMap
    private var carMarker: Marker? = null
    private var drawedLegs: Map<Leg, Polyline> = emptyMap()
    private var drawedObstacles: Map<Coordinate, List<Marker>> = Collections.emptyMap()
    private lateinit var reactiveSensor: ReactiveSensor
    private lateinit var reactiveLocation: ReactiveLocation
    private lateinit var sensorCombiners: SensorCombiners
    private var currentCameraBearing = BEARING
    private lateinit var spotFAB: FloatingActionButton
    private var spotting = false
    private var backendService: SpotholeService? = null
    private lateinit var lastPositionFetched: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        checkPermissions()
        initServices()
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        spotFAB = findViewById(R.id.fab_spot)
        spotFAB.isEnabled = false
        spotFAB.setOnClickListener {
            toggleSpotService()
        }
    }

    private fun checkPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            1
        )
    }

    private fun initServices() {
        val baseUrl = if (BuildConfig.DEBUG) {
            getString(R.string.spothole_service_development_baseurl)
        } else {
            getString(R.string.spothole_service_production_baseurl)
        }

        val retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
        backendService = retrofit.create(SpotholeService::class.java)

        reactiveSensor = ReactiveSensor(applicationContext)
        reactiveLocation = ReactiveLocation(applicationContext)
        sensorCombiners = SensorCombiners(reactiveLocation, reactiveSensor)
    }

    private fun toggleSpotService() {
        if (spotting) {
            Toast.makeText(
                applicationContext, getString(R.string.road_scan_stopped), Toast.LENGTH_SHORT
            ).show()
            spotFAB.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(applicationContext, R.color.primaryColor)
            )
            spotFAB.setImageDrawable(getDrawable(R.drawable.ic_near_me_white));
        } else {
            Toast.makeText(
                applicationContext, getString(R.string.road_scan_started), Toast.LENGTH_SHORT
            ).show()
            spotFAB.backgroundTintList = ColorStateList.valueOf(getColor(R.color.secondaryColor))
            spotFAB.setImageDrawable(getDrawable(R.drawable.ic_pan_tool_white_24dp));
            fetchNewData(carMarker!!.position, DEFAULT_RADIUS_METERS)
        }
        spotting = !spotting
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
        mMap.mapType = GoogleMap.MAP_TYPE_NONE
        mMap.addTileOverlay(
            TileOverlayOptions().tileProvider(CustomTileProvider()).zIndex(-1f).fadeIn(true)
        )
        carMarker = addCarMarker(DEFAULT_LOCATION, BEARING)
        spotFAB.isEnabled = true //avoid user start spotting before map is ready.
        startSensorObservers()
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

    private fun drawLeg(leg: Leg) {
        runOnUiThread {
            val polyline = mMap.addPolyline(
                PolylineOptions().addAll(
                    listOf(
                        leg.from.coordinates.toLatLng(),
                        leg.to.coordinates.toLatLng()
                    )
                )
                    .color(Quality.fromValue(leg.quality.roundToInt())!!.color.toArgb())
                    .startCap(RoundCap()).endCap(RoundCap())
            )
            drawedLegs += leg to polyline
        }
        leg.obstacles.forEach { (obsType, obsCoordinateList) ->
            obsCoordinateList.forEach { coordinate ->
                drawObstacle(
                    obsType,
                    coordinate
                )
            }
        }
    }

    private fun drawObstacle(obstacleType: ObstacleType, coordinate: Coordinate) {
        val drawable = when (obstacleType) {
            ObstacleType.POTHOLE -> R.drawable.ic_up_arrow_circle
            ObstacleType.MANHOLE -> R.drawable.ic_up_arrow_circle
            ObstacleType.SPEED_BUMP -> R.drawable.ic_up_arrow_circle
            ObstacleType.JOINT -> R.drawable.ic_up_arrow_circle

        }
        val circleDrawable = ContextCompat.getDrawable(applicationContext, drawable)
        val markerIcon = DrawableUtils.getMarkerIconFromDrawable(circleDrawable!!)

        runOnUiThread {
            val obstacleMarker = mMap.addMarker(
                MarkerOptions()
                    .position(coordinate.toLatLng())
                    .anchor(0.5f, 0.5f)
                    .icon(markerIcon)
            )
            drawedObstacles += coordinate to listOf(obstacleMarker) + (drawedObstacles.getOrElse(
                coordinate,
                { -> listOf<Marker>() }))
        }
    }

    private fun updateCarLocation(position: LatLng) {
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

                if (t > 1.0) { //end the animation on the right place
                    updateCameraLocation(position, bearing = currentCameraBearing)
                    runOnUiThread { carMarker!!.position = position }
                } else {
                    val lng: Double = t * position.longitude + (1 - t) * startLatLng.longitude
                    val lat: Double = t * position.latitude + (1 - t) * startLatLng.latitude
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
            .build()
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


    /*
        Camera must adapt its orientation so that the marker is always facing upwards,
        this requires that the camera continuously change its orientation as soon as the delta
        between the two angles is above a certain threshold.
    */
    private var isRotating = false
    private var currentCarTargetRotation = BEARING
    private val duration: Long = 500
    private val carRotationHandler = Handler()
    private fun updateRotation(newTargetRotation: Float) {
        if (MapUtils.rotationGap(
                currentCarTargetRotation,
                newTargetRotation
            ) > DELTA_TRIGGER_ROTATION && !isRotating
        ) {
            currentCarTargetRotation = newTargetRotation
            val start = SystemClock.uptimeMillis()
            val currentRotation = carMarker!!.rotation
            val interpolator = LinearInterpolator()

            val rotateCar = object : Runnable {
                override fun run() {
                    isRotating = true
                    val elapsed = SystemClock.uptimeMillis() - start
                    val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                    val newRotation: Float =
                        t * currentCarTargetRotation + (1 - t) * currentRotation

                    if (t < 1.0) { // Post again 16ms later.
                        runOnUiThread { carMarker!!.rotation = newRotation }
                        carRotationHandler.postDelayed(this, 16)
                    } else {
                        isRotating = false
                    }
                }
            }

            if (MapUtils.rotationGap(
                    mMap.cameraPosition.bearing,
                    newTargetRotation
                ) < DELTA_CAMERA_ROTATION
            ) {
                Log.d("MapActivity", "Rotating only the car")
                carRotationHandler.post(rotateCar)
            } else {
                Log.d("MapActivity", "Rotating everything")
                carRotationHandler.post(rotateCar)
                updateCameraLocation(location = carMarker!!.position, bearing = newTargetRotation)
            }
        }
    }

    private fun startSensorObservers() {

        reactiveLocation.observe().observeOn(AndroidSchedulers.mainThread())
            .map { LatLng(it.latitude, it.longitude) }.subscribe {
                updateCarLocation(it)
                val actualRadius = MapUtils.getVisibleRadius(mMap.projection.visibleRegion)
                if (spotting && (!this::lastPositionFetched.isInitialized ||
                    MapUtils.distBetween(
                        lastPositionFetched, it
                    ) > actualRadius / 2)
                ) {
                    fetchNewData(it,
                        (2 * actualRadius).coerceAtMost(MAX_RADIUS_METERS)
                    )
                }
        }
        reactiveSensor.observer(SensorType.ROTATION_VECTOR)
            .observeOn(AndroidSchedulers.mainThread()).map(OrientationMapper()).subscribe {
                updateRotation(it);
            }

        sensorCombiners.combineByStretchLength()
            .filter { spotting }
            .buffer(BUFFER_SIZE, BUFFER_SIZE - 1)
            .subscribe({
                Log.i("DummyBuffer", it.map { value ->
                    String.format(
                        "DUMMY: %s, <%d:%d>",
                        value.timestamp,
                        value.location?.latitude,
                        value.location?.longitude
                    )
                }.joinToString(separator = "\n"))
            }, {
                it.printStackTrace()
            })


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

    private fun fetchNewData(location: LatLng, radius: Float) {
        lastPositionFetched = location
        backendService!!.loadEvaluationsFromUserPerspective(
            location.latitude,
            location.longitude,
            radius
        )
            .subscribeOn(Schedulers.single()).subscribe({
                it.forEach { leg -> drawLeg(leg) }
            }, {
                Log.e("SpotService", "An error occurred while fetching new data: $it")
            })
    }


}
