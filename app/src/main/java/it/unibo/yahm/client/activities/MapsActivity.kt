package it.unibo.yahm.client.activities

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Point
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.SphericalUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import it.unibo.yahm.R
import it.unibo.yahm.client.SpotholeService
import it.unibo.yahm.client.entities.Coordinate
import it.unibo.yahm.client.entities.Leg
import it.unibo.yahm.client.entities.ObstacleType
import it.unibo.yahm.client.entities.Quality
import it.unibo.yahm.client.sensors.*
import it.unibo.yahm.client.services.RetrofitService
import it.unibo.yahm.client.services.RoadClassifiersService
import it.unibo.yahm.client.utils.ScreenUtils
import it.unibo.yahm.client.utils.CustomTileProvider
import it.unibo.yahm.client.utils.DrawableUtils
import it.unibo.yahm.client.utils.MapUtils
import it.unibo.yahm.client.utils.MapUtils.Companion.distBetween
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var carMarker: Marker? = null
    private var drawedLegs: Map<Leg, Polyline> = emptyMap()
    private var drawedObstacles: Map<Coordinate, List<Marker>> = Collections.emptyMap()
    private var obstaclesTypeAndCoordinates: Map<ObstacleType, List<Coordinate>> = Collections.emptyMap()
    private lateinit var reactiveSensor: ReactiveSensor
    private lateinit var reactiveLocation: ReactiveLocation
    private lateinit var sensorCombiners: SensorCombiners
    private var currentCameraBearing = BEARING
    private lateinit var spotFAB: FloatingActionButton
    private var spotting = false
    private var isAudioOn = true
    private var mapReady = false
    private lateinit var backendService: SpotholeService
    private lateinit var lastPositionFetched: LatLng
    private var holeMediaPlayer: MediaPlayer? = null
    private var speedBumpMediaPlayer: MediaPlayer? = null
    private lateinit var roadClassifiersService: RoadClassifiersService
    private val screenSize = Point()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        spotFAB = findViewById(R.id.fab_spot)
        spotFAB.isEnabled = false
        spotFAB.setOnClickListener {
            toggleSpotService()
        }

        windowManager.defaultDisplay.getSize(screenSize)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.maps_menu, menu)
        return true
    }

    override fun onStart() {
        super.onStart()
        Log.d("CYCLE", "onStart")
        restorePreferences()
    }

    override fun onResume() {
        super.onResume()
        Log.d("CYCLE", "onResume")
        invalidateOptionsMenu()
        initMediaPlayers()
        initServices()
        startSensorObservers()
    }

    override fun onPause() {
        super.onPause()
        Log.d("CYCLE", "onPause")
        releaseMediaPlayers()
        stopSpottingService()
        stopSensorObservers()
    }

    override fun onStop() {
        super.onStop()
        Log.d("CYCLE", "onStop")
        savePreferences()
    }


    private fun restorePreferences() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        isAudioOn = sharedPref.getBoolean(getString(R.string.VOLUME_STATE_KEY), true)
    }

    private fun savePreferences() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putBoolean(getString(R.string.VOLUME_STATE_KEY), isAudioOn)
            commit()
        }
    }

    private fun initMediaPlayers() {
        holeMediaPlayer = MediaPlayer.create(this, R.raw.it_pothole_female)
        speedBumpMediaPlayer = MediaPlayer.create(this, R.raw.it_speedbump_female)
    }

    private fun releaseMediaPlayers() {
        holeMediaPlayer?.release()
        speedBumpMediaPlayer?.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId

        if (id == R.id.toggleAudio) {
            toggleAudio()
            setAudioSwitchIcon(item)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.toggleAudio)
        setAudioSwitchIcon(item)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setAudioSwitchIcon(item: MenuItem) {
        if (isAudioOn) {
            item.icon = getDrawable(R.drawable.ic_volume_up_solid)
        } else {
            item.icon = getDrawable(R.drawable.ic_volume_off_solid)
        }
    }

    private fun initServices() {
        reactiveSensor = ReactiveSensor(applicationContext)
        reactiveLocation = ReactiveLocation(applicationContext)
        sensorCombiners = SensorCombiners(reactiveLocation, reactiveSensor)
        backendService = RetrofitService(applicationContext).spotholeService
        roadClassifiersService = RoadClassifiersService(applicationContext, reactiveSensor, reactiveLocation, backendService)
    }

    private fun toggleAudio() {
        if (isAudioOn) {
            holeMediaPlayer?.setVolume(0f, 0f)
            speedBumpMediaPlayer?.setVolume(0f, 0f)
        } else {
            holeMediaPlayer?.setVolume(1f, 1f)
            speedBumpMediaPlayer?.setVolume(1f, 1f)
        }
        holeMediaPlayer?.seekTo(0)
        speedBumpMediaPlayer?.seekTo(0)
        isAudioOn = !isAudioOn
    }

    private fun toggleSpotService() {
        if (spotting) {
            stopSpottingService()
        } else {
            startSpottingService()
        }
    }

    private fun startSpottingService() {
        if (!spotting) {
            Toast.makeText(
                    applicationContext, getString(R.string.road_scan_started), Toast.LENGTH_SHORT
            ).show()
            spotFAB.backgroundTintList = ColorStateList.valueOf(getColor(R.color.secondaryColor))
            spotFAB.setImageDrawable(getDrawable(R.drawable.ic_pan_tool_white_24dp))
            fetchNewData(carMarker!!.position, DEFAULT_RADIUS_METERS)
            roadClassifiersService.startService()
            ScreenUtils.setAlwaysOn(this, true)
            spotting = true
        }
    }

    private fun stopSpottingService() {
        if (spotting) {
            Toast.makeText(
                    applicationContext, getString(R.string.road_scan_stopped), Toast.LENGTH_SHORT
            ).show()
            spotFAB.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(applicationContext, R.color.primaryColor)
            )
            spotFAB.setImageDrawable(getDrawable(R.drawable.ic_time_to_leave_white_24dp))
            roadClassifiersService.stopService()
            ScreenUtils.setAlwaysOn(this, false)
            spotting = false
        }

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
        Log.d("CYCLE", "onMapReady")
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NONE
        mMap.addTileOverlay(
                TileOverlayOptions().tileProvider(CustomTileProvider()).zIndex(-1f).fadeIn(true)
        )
        spotFAB.isEnabled = true //avoid user start spotting before map is ready.
        mapReady = true
    }


    private fun drawLeg(leg: Leg) {
        if (!mapReady) return
        runOnUiThread {
            Log.d(javaClass.name, "Drawing leg from ${leg.from.id} to ${leg.to.id} with quality ${leg.quality}")
            val polyline = mMap.addPolyline(
                    PolylineOptions().addAll(listOf(leg.from.coordinates.toLatLng(), leg.to.coordinates.toLatLng()))
                            .color(Quality.fromValue(leg.quality)!!.color)
                            .startCap(RoundCap()).endCap(RoundCap())
            )
            drawedLegs = drawedLegs + (leg to polyline)
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
        if (!mapReady) return
        val drawable = when (obstacleType) {
            ObstacleType.NOTHING -> null
            ObstacleType.POTHOLE -> R.drawable.ic_pothole_marker
            ObstacleType.SPEED_BUMP -> R.drawable.ic_speedbump_marker
        }

        if (drawable != null) {
            val circleDrawable = ContextCompat.getDrawable(applicationContext, drawable)
            val markerIcon = DrawableUtils.getMarkerIconFromDrawable(circleDrawable!!)

            Log.d(javaClass.name, "Drawing obstacle of type $obstacleType at $coordinate")

            runOnUiThread {
                val obstacleMarker = mMap.addMarker(
                        MarkerOptions()
                                .position(coordinate.toLatLng())
                                .anchor(0.5f, 1f)
                                .icon(markerIcon)
                )
                drawedObstacles =
                        drawedObstacles + (coordinate to listOf(obstacleMarker) + (drawedObstacles.getOrElse(
                                coordinate,
                                {
                                    emptyList()
                                })))
            }
        }
    }

    private var stopPreviousInterpolation = AtomicBoolean(false)
    private fun updateCarLocation(position: LatLng) {
        if (!mapReady) return
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val projection: Projection = mMap.projection
        val startPoint: Point = projection.toScreenLocation(carMarker!!.position)
        val startLatLng = projection.fromScreenLocation(startPoint)
        val duration: Long = 1000
        val interpolator = LinearInterpolator()

        val finalScreenPosition = projection.toScreenLocation(position)
        if(reactiveSensor.isDisposed(SensorType.ROTATION_VECTOR)){
            val newBearing = SphericalUtil.computeHeading(startLatLng, position).toFloat()
            updateRotation(newBearing)
            if(abs(newBearing) - abs(currentCameraBearing) < -15f
                    || abs(newBearing) - abs(currentCameraBearing) > 15f
                    || finalScreenPosition.x < SCREEN_PADDING || finalScreenPosition.x > screenSize.x - SCREEN_PADDING
                    || finalScreenPosition.y < SCREEN_PADDING || finalScreenPosition.y > screenSize.y - SCREEN_PADDING) {
                updateCameraLocation(position, bearing = newBearing)
            }
            currentCameraBearing = newBearing
        }
        stopPreviousInterpolation.set(true)

        fun interpolate(stopInterpolation: AtomicBoolean): Runnable {
            return object : Runnable {
                override fun run() {
                    val elapsed = SystemClock.uptimeMillis() - start
                    val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)

                    val lng: Double = t * position.longitude + (1 - t) * startLatLng.longitude
                    val lat: Double = t * position.latitude + (1 - t) * startLatLng.latitude
                    val pos = LatLng(lat, lng)
                    runOnUiThread { carMarker!!.position = pos }

                    if (t < 1.0 && !stopInterpolation.get()) {
                        handler.postDelayed(this, 16)
                    }
                }
            }
        }

        stopPreviousInterpolation = AtomicBoolean(false)
        handler.post(interpolate(stopPreviousInterpolation))
    }

    private fun updateCameraLocation(location: LatLng, zoom: Float = ZOOM, tilt: Float = TILT, bearing: Float = BEARING) {
        if (!mapReady) return
        val cameraUpdate = CameraPosition.Builder()
                .target(location)
                .zoom(zoom)
                .tilt(tilt)
                .bearing(bearing)
                .build()
        currentCameraBearing = bearing
        Log.d(javaClass.name, "Update camera location: $location, bearing: $bearing")
        runOnUiThread { mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraUpdate)) }
    }

    private var isRotating = false
    private var currentCarTargetRotation = BEARING
    private fun updateRotation(newTargetRotation: Float) {
        if (!mapReady) return
        if (MapUtils.rotationGap(currentCarTargetRotation, newTargetRotation) > DELTA_TRIGGER_ROTATION) {
            Log.d(javaClass.name, "Rotating car from $currentCarTargetRotation to $newTargetRotation")
            currentCarTargetRotation = newTargetRotation
            carMarker!!.rotation = newTargetRotation
            //rotateMarker(carMarker!!, newTargetRotation)
            /* if (MapUtils.rotationGap(mMap.cameraPosition.bearing, newTargetRotation) >= DELTA_CAMERA_ROTATION) {
                 updateCameraLocation(location = carMarker!!.position, bearing = newTargetRotation)
             }*/
        }
    }

    /*private var stopPreviousRotationInterpolation = AtomicBoolean(false)
    private fun rotateMarker(marker: Marker, toRotation: Float) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val startRotation = marker.rotation
        val duration: Long = 100
        val interpolator = LinearInterpolator()

        stopPreviousRotationInterpolation.set(true)

        fun interpolate(stopInterpolation: AtomicBoolean): Runnable {
            return object : Runnable {
                override fun run() {
                    isRotating = true
                    val elapsed = SystemClock.uptimeMillis() - start
                    val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                    val rot = round(t * toRotation + (1 - t) * startRotation)
                    marker.rotation = (if (-rot > 180) rot / 2 else rot)
                    if (t < 1.0 && !stopInterpolation.get()) {
                        handler.postDelayed(this, 16)
                    }
                }
            }
        }

        stopPreviousRotationInterpolation = AtomicBoolean(false)
        handler.post(interpolate(stopPreviousRotationInterpolation))
    }*/

    private fun signalObstacle(latLng: LatLng, speed: Float) {
        synchronized(obstaclesTypeAndCoordinates) {
            obstaclesTypeAndCoordinates = obstaclesTypeAndCoordinates.entries.map { entry ->
                val newList = mutableListOf<Coordinate>()
                entry.value.forEach { coordinates ->
                    val distanceBetweenCarAndObstacle = distBetween(latLng, coordinates.toLatLng())
                    if (
                            if (speed > 0.0f)
                                distanceBetweenCarAndObstacle / speed < OBSTACLE_ALARM_THRESHOLD_IN_SECONDS
                            else
                                distanceBetweenCarAndObstacle < OBSTACLE_ALARM_THRESHOLD_IN_METERS
                    ) {
                        if (entry.key == ObstacleType.POTHOLE && holeMediaPlayer != null) {
                            holeMediaPlayer?.start()
                        } else if (entry.key == ObstacleType.SPEED_BUMP && speedBumpMediaPlayer != null) {
                            speedBumpMediaPlayer?.start()
                        }
                        Log.d(javaClass.name, "Playing sound for ${entry.key}")
                    } else {
                        newList.add(coordinates)
                    }
                }
                entry.key to newList
            }.toMap()
        }
    }

    private fun fetchNewDataIfIsNeeded(latLng: LatLng) {
        val actualRadius = MapUtils.getVisibleRadius(mMap.projection.visibleRegion)

        if (spotting && (!this::lastPositionFetched.isInitialized ||
                        distBetween(
                                lastPositionFetched, latLng
                        ) > actualRadius / 2)
        ) {
            fetchNewData(
                    latLng,
                    (2 * actualRadius).coerceAtMost(MAX_RADIUS_METERS)
            )
        }
    }

    private fun startSensorObservers() {
        val locationObserver = reactiveLocation.observe().observeOn(AndroidSchedulers.mainThread())
        locationObserver
                .filter { it.hasAccuracy() && it.accuracy < 100 && mapReady && carMarker != null}
                .subscribe {
                    val latLng = LatLng(it.latitude, it.longitude)
                    if(it.hasAccuracy() && it.accuracy < 45f && !reactiveSensor.isDisposed(SensorType.ROTATION_VECTOR)){
                        reactiveSensor.dispose(SensorType.ROTATION_VECTOR)
                    }
                    updateCarLocation(latLng)
                    signalObstacle(latLng, it.speed)
                    fetchNewDataIfIsNeeded(latLng)
                }

        locationObserver
                .filter { it.hasAccuracy() && it.accuracy < 50 && mapReady }
                .take(1)
                .subscribe {
                    val circleDrawable =
                            ContextCompat.getDrawable(applicationContext, R.drawable.ic_up_arrow_circle)
                    val markerIcon = DrawableUtils.getMarkerIconFromDrawable(circleDrawable!!)
                    val latLng = LatLng(it.latitude, it.longitude)
                    if(carMarker == null) {
                        carMarker = mMap.addMarker(
                                MarkerOptions()
                                        .position(latLng)
                                        .anchor(0.5f, 0.5f)
                                        .rotation(BEARING)
                                        .flat(true)
                                        .icon(markerIcon)
                        )
                    }
                    updateCameraLocation(LatLng(it.latitude, it.longitude), bearing = 0f)
                }

        reactiveSensor.observer(SensorType.ROTATION_VECTOR)
                .observeOn(AndroidSchedulers.mainThread()).filter{mapReady && carMarker != null}.map(OrientationMapper()).subscribe {
                    updateRotation(it)
                }
    }

    private fun stopSensorObservers() {
        reactiveLocation.dispose()
        if(!reactiveSensor.isDisposed(SensorType.ROTATION_VECTOR)){
            reactiveSensor.dispose(SensorType.ROTATION_VECTOR)
        }
    }

    private fun fetchNewData(location: LatLng, radius: Float) {
        lastPositionFetched = location
        backendService.loadEvaluationsFromUserPerspective(
                location.latitude,
                location.longitude,
                radius
        ).subscribeOn(Schedulers.single()).subscribe({ legs ->
            legs.forEach { leg ->
                synchronized(obstaclesTypeAndCoordinates) {
                    obstaclesTypeAndCoordinates = (obstaclesTypeAndCoordinates.asSequence() + leg.obstacles.asSequence())
                            .groupBy({ it.key }, { it.value })
                            .mapValues { (_, values) -> values.flatten() }
                }
                drawLeg(leg)
            }
        }, {
            Log.e("SpotService", "An error occurred while fetching new data: $it")
        })
    }

    companion object {
        private val DEFAULT_LOCATION = LatLng(45.133331, 12.233333)
        private const val DELTA_TRIGGER_ROTATION: Int = 15
        private const val DELTA_CAMERA_ROTATION: Int = 45
        private const val ZOOM: Float = 18f
        private const val TILT: Float = 45f
        private const val BEARING: Float = 0f
        private const val MAX_RADIUS_METERS = 2000f //won't ask for disease further away than that
        private const val DEFAULT_RADIUS_METERS = MAX_RADIUS_METERS / 10
        private const val OBSTACLE_ALARM_THRESHOLD_IN_SECONDS = 3
        private const val OBSTACLE_ALARM_THRESHOLD_IN_METERS = 150
        private const val SCREEN_PADDING = 500
    }

}
