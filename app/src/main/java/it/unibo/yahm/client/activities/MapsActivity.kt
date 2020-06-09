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
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.SphericalUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import it.unibo.yahm.R
import it.unibo.yahm.client.SpotholeService
import it.unibo.yahm.client.entities.*
import it.unibo.yahm.client.sensors.*
import it.unibo.yahm.client.services.RetrofitService
import it.unibo.yahm.client.services.RoadClassifiersService
import it.unibo.yahm.client.utils.CustomTileProvider
import it.unibo.yahm.client.utils.DrawableUtils
import it.unibo.yahm.client.utils.MapUtils
import it.unibo.yahm.client.utils.MapUtils.Companion.distBetween
import it.unibo.yahm.client.utils.ScreenUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var carMarker: Marker? = null
    private var legsCache: LruCache<Leg, Long> = LruCache(MAX_CACHE_SIZE)
    private var obstaclesCache: LruCache<Obstacle, Boolean> = LruCache(MAX_CACHE_SIZE)
    private lateinit var reactiveSensor: ReactiveSensor
    private lateinit var reactiveLocation: ReactiveLocation
    private lateinit var sensorCombiners: SensorCombiners
    private var currentCameraBearing = 0f
    private lateinit var spotFAB: FloatingActionButton
    private var spotting = false
    private var isAudioOn = true
    private var mapReady = false
    private lateinit var backendService: SpotholeService
    private var lastPositionFetched: LatLng? = null
    private var holeMediaPlayer: MediaPlayer? = null
    private var speedBumpMediaPlayer: MediaPlayer? = null
    private lateinit var roadClassifiersService: RoadClassifiersService
    private val screenSize = Point()
    private val fetchThread = Schedulers.newThread()
    private var updateRotationWithSensors = false
    private var stopPreviousInterpolation = AtomicBoolean(false)
    private var currentCarTargetRotation = 0f


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
        restorePreferences()
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
        initMediaPlayers()
        initServices()
        startSensorObservers()
    }

    override fun onPause() {
        super.onPause()
        releaseMediaPlayers()
        stopSpottingService()
        stopSensorObservers()
    }

    override fun onStop() {
        super.onStop()
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
        spotting = !spotting
    }

    private fun startSpottingService() {
        Toast.makeText(applicationContext, getString(R.string.road_scan_started), Toast.LENGTH_SHORT).show()
        spotFAB.backgroundTintList = ColorStateList.valueOf(getColor(R.color.secondaryColor))
        spotFAB.setImageDrawable(getDrawable(R.drawable.ic_pan_tool_white_24dp))
        fetchNewData(carMarker!!.position, DEFAULT_RADIUS_METERS)
        roadClassifiersService.startService()
        ScreenUtils.setAlwaysOn(this, true)
    }

    private fun stopSpottingService() {
        Toast.makeText(applicationContext, getString(R.string.road_scan_stopped), Toast.LENGTH_SHORT).show()
        spotFAB.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(applicationContext, R.color.primaryColor))
        spotFAB.setImageDrawable(getDrawable(R.drawable.ic_time_to_leave_white_24dp))
        roadClassifiersService.stopService()
        ScreenUtils.setAlwaysOn(this, false)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(javaClass.name, "Map ready.")
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NONE
        mMap.addTileOverlay(TileOverlayOptions().tileProvider(CustomTileProvider()).zIndex(-1f).fadeIn(true))
        spotFAB.isEnabled = true //avoid user start spotting before map is ready.
        mapReady = true
    }

    private fun drawLeg(leg: Leg) {
        if (!mapReady) return

        if (legsCache.put(leg, SystemClock.uptimeMillis()) == null) {
            Log.v(javaClass.name, "Drawing leg from ${leg.from.id} to ${leg.to.id} with quality ${leg.quality}")
            val polyline = PolylineOptions()
                .addAll(listOf(leg.from.coordinates.toLatLng(), leg.to.coordinates.toLatLng()))
                .color(Quality.fromValue(leg.quality)!!.color)
                .startCap(RoundCap())
                .endCap(RoundCap())

            runOnUiThread {
                mMap.addPolyline(polyline)
            }
        } else {
            Log.v(javaClass.name, "Leg from ${leg.from.id} to ${leg.to.id} already drawn")
        }

        leg.obstacles.forEach { (obstacleType, obsCoordinateList) ->
            obsCoordinateList.forEach { drawObstacle(Obstacle(it, obstacleType)) }
        }
    }

    private fun drawObstacle(obstacle: Obstacle) {
        if (!mapReady) return

        if (obstaclesCache.put(obstacle, false) != null) {
            Log.v(javaClass.name, "Obstacle of type ${obstacle.obstacleType} at ${obstacle.coordinates} already drawn")
            return
        }

        val drawable = when (obstacle.obstacleType) {
            ObstacleType.NOTHING -> null
            ObstacleType.POTHOLE -> R.drawable.ic_pothole_marker
            ObstacleType.SPEED_BUMP -> R.drawable.ic_speedbump_marker
        }

        if (drawable != null) {
            val circleDrawable = ContextCompat.getDrawable(applicationContext, drawable)
            val markerIcon = DrawableUtils.getMarkerIconFromDrawable(circleDrawable!!)

            Log.v(javaClass.name, "Drawing obstacle of type ${obstacle.obstacleType} at ${obstacle.coordinates}")
            val marker = MarkerOptions().position(obstacle.coordinates.toLatLng())
                .anchor(0.5f, 1f).icon(markerIcon)
            runOnUiThread {
                mMap.addMarker(marker)
            }
            obstaclesCache.put(obstacle, true) // force to be played
        }
    }

    private fun updateCarLocation(position: LatLng) {
        if (!mapReady) return
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val projection: Projection = mMap.projection
        val startPoint: Point = projection.toScreenLocation(carMarker!!.position)
        val startLatLng = projection.fromScreenLocation(startPoint)
        val interpolator = LinearInterpolator()

        if (!updateRotationWithSensors) {
            val finalScreenPosition = projection.toScreenLocation(position)
            val newBearing = SphericalUtil.computeHeading(startLatLng, position).toFloat()
            updateRotation(newBearing)
            if (abs(newBearing - currentCameraBearing) > CAMERA_BEARING_THRESHOLD ||
                finalScreenPosition.x < SCREEN_PADDING || finalScreenPosition.x > screenSize.x - SCREEN_PADDING ||
                finalScreenPosition.y < SCREEN_PADDING || finalScreenPosition.y > screenSize.y - SCREEN_PADDING) {
                updateCameraLocation(position, newBearing)
            }
            currentCameraBearing = newBearing
        }
        stopPreviousInterpolation.set(true)

        fun interpolate(stopInterpolation: AtomicBoolean): Runnable {
            return object : Runnable {
                override fun run() {
                    val elapsed = SystemClock.uptimeMillis() - start
                    val t: Float = interpolator.getInterpolation(elapsed.toFloat() / INTERPOLATE_DURATION)

                    val lng: Double = t * position.longitude + (1 - t) * startLatLng.longitude
                    val lat: Double = t * position.latitude + (1 - t) * startLatLng.latitude
                    val pos = LatLng(lat, lng)
                    runOnUiThread { carMarker!!.position = pos }

                    if (t < 1.0 && !stopInterpolation.get()) {
                        handler.postDelayed(this, INTERPOLATE_INTERVAL)
                    }
                }
            }
        }

        stopPreviousInterpolation = AtomicBoolean(false)
        handler.post(interpolate(stopPreviousInterpolation))
    }

    private fun updateCameraLocation(location: LatLng, bearing: Float, zoom: Float = ZOOM, tilt: Float = TILT) {
        if (!mapReady) return
        val cameraUpdate = CameraPosition.Builder()
                .target(location)
                .zoom(zoom)
                .tilt(tilt)
                .bearing(bearing)
                .build()
        Log.d(javaClass.name, "Update camera location: $location, bearing: $bearing")
        runOnUiThread { mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraUpdate)) }
    }


    private fun updateRotation(newTargetRotation: Float) {
        if (!mapReady) return
        if (MapUtils.rotationGap(currentCarTargetRotation, newTargetRotation) > DELTA_TRIGGER_ROTATION) {
            Log.d(javaClass.name, "Rotating car marker from $currentCarTargetRotation to $newTargetRotation")
            currentCarTargetRotation = newTargetRotation
            carMarker!!.rotation = newTargetRotation
        }
    }

    private fun signalObstacle(latLng: LatLng, speed: Float) {
        obstaclesCache.snapshot()
            .filter { it.value } // to be played
            .filter {
                val distanceBetweenCarAndObstacle = distBetween(latLng, it.key.coordinates.toLatLng())
                if (speed > 0.0f) distanceBetweenCarAndObstacle / speed < OBSTACLE_ALARM_THRESHOLD_IN_SECONDS
                else distanceBetweenCarAndObstacle < OBSTACLE_ALARM_THRESHOLD_IN_METERS
            }.forEach {
                if (it.key.obstacleType == ObstacleType.POTHOLE) {
                    holeMediaPlayer?.start()
                } else if (it.key.obstacleType == ObstacleType.SPEED_BUMP) {
                    speedBumpMediaPlayer?.start()
                }
                Log.d(javaClass.name, "Playing sound for ${it.key}")
                obstaclesCache.put(it.key, false)
            }
    }

    private fun fetchNewDataIfIsNeeded(latLng: LatLng) {
        val actualRadius = MapUtils.getVisibleRadius(mMap.projection.visibleRegion)

        if (spotting &&
            (lastPositionFetched == null || distBetween(lastPositionFetched!!, latLng) > actualRadius / 2)) {
            fetchNewData(latLng, (2 * actualRadius).coerceAtMost(MAX_RADIUS_METERS))
        }
    }

    private fun startSensorObservers() {
        updateRotationWithSensors = true

        val locationObserver = reactiveLocation.observe().observeOn(AndroidSchedulers.mainThread())
        locationObserver
            .filter { it.hasAccuracy() && it.accuracy < GPS_ACCURACY_THRESHOLD && mapReady && carMarker != null}
            .subscribe {
                if (updateRotationWithSensors) {
                    Log.v(javaClass.name, "Disposing rotation vector sensor..")
                    updateRotationWithSensors = false
                    reactiveSensor.dispose(SensorType.ROTATION_VECTOR)
                }
                val latLng = LatLng(it.latitude, it.longitude)
                updateCarLocation(latLng)
                fetchNewDataIfIsNeeded(latLng)
                signalObstacle(latLng, it.speed)
            }

        locationObserver
            .filter { it.hasAccuracy() && it.accuracy < INITIAL_GPS_ACCURACY_THRESHOLD && mapReady }
            .take(1)
            .subscribe {
                val circle = ContextCompat.getDrawable(applicationContext, R.drawable.ic_up_arrow_circle)
                val markerIcon = DrawableUtils.getMarkerIconFromDrawable(circle!!)
                val latLng = LatLng(it.latitude, it.longitude)
                if (carMarker == null) {
                    Log.v(javaClass.name, "Adding car marker..")
                    carMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .anchor(0.5f, 0.5f)
                            .rotation(0f)
                            .flat(true)
                            .icon(markerIcon)
                    )
                }
                updateCameraLocation(latLng, 0f)
            }

        reactiveSensor.observer(SensorType.ROTATION_VECTOR)
            .observeOn(AndroidSchedulers.mainThread())
            .filter{ mapReady && carMarker != null }
            .map(OrientationMapper())
            .subscribe {
                updateRotation(it)
            }
    }

    private fun stopSensorObservers() {
        reactiveLocation.dispose()
        if (updateRotationWithSensors) {
            reactiveSensor.dispose(SensorType.ROTATION_VECTOR)
        }
    }

    private fun fetchNewData(location: LatLng, radius: Float) {
        lastPositionFetched = location
        backendService.loadEvaluationsFromUserPerspective(location.latitude, location.longitude, radius)
            .subscribeOn(fetchThread)
            .subscribe({ legs ->
                legs.forEach { drawLeg(it) }
            }, {
                Log.w("SpotService", "Can't fetch new data: $it")
            })
    }

    companion object {
        private const val DELTA_TRIGGER_ROTATION: Int = 15
        private const val ZOOM: Float = 18f
        private const val TILT: Float = 45f
        private const val MAX_RADIUS_METERS = 2000f //won't ask for disease further away than that
        private const val DEFAULT_RADIUS_METERS = MAX_RADIUS_METERS / 10
        private const val OBSTACLE_ALARM_THRESHOLD_IN_SECONDS = 3
        private const val OBSTACLE_ALARM_THRESHOLD_IN_METERS = 150
        private const val SCREEN_PADDING = 500
        private const val MAX_CACHE_SIZE = 1024*1024
        private const val INTERPOLATE_INTERVAL = 16L
        private const val INTERPOLATE_DURATION = 1000
        private const val CAMERA_BEARING_THRESHOLD = 15f
        private const val INITIAL_GPS_ACCURACY_THRESHOLD = 50f
        private const val GPS_ACCURACY_THRESHOLD = 10f
    }

}
