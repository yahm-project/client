package it.unibo.yahm.client.activities

import android.content.res.ColorStateList
import android.graphics.Point
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import cartago.INTERNAL_OPERATION
import cartago.OPERATION
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.SphericalUtil
import it.unibo.pslab.jaca_android.core.ActivityArtifact
import it.unibo.pslab.jaca_android.core.JaCaBaseActivity
import it.unibo.yahm.R
import it.unibo.yahm.client.entities.Leg
import it.unibo.yahm.client.entities.Obstacle
import it.unibo.yahm.client.entities.ObstacleType
import it.unibo.yahm.client.entities.Quality
import it.unibo.yahm.client.sensors.GpsLocation
import it.unibo.yahm.client.utils.CustomTileProvider
import it.unibo.yahm.client.utils.DrawableUtils
import it.unibo.yahm.client.utils.MapUtils
import it.unibo.yahm.client.utils.ScreenUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs


class NavigatorActivity : ActivityArtifact(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var holeMediaPlayer: MediaPlayer? = null
    private var speedBumpMediaPlayer: MediaPlayer? = null
    private var mapReady = false
    private var carMarker: Marker? = null
    private var currentCarTargetRotation = 0f
    private var updateRotationWithSensors = false
    private var stopPreviousInterpolation = AtomicBoolean(false)
    private var currentCameraBearing = 0f
    private val screenSize = Point()
    private val alreadySignaled = mutableListOf<Obstacle>()
    private lateinit var navigationSupportFAB: FloatingActionButton
    private var isSupportEnable = false
    private var wrappedActivity: NavigatorMain? = null

    class NavigatorMain : JaCaBaseActivity() {
        fun loadMap(callback: OnMapReadyCallback) {
            (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(
                    callback
            )
        }

        fun keepScreenOn() {
            ScreenUtils.setAlwaysOn(this, true)
        }

        fun disableKeepScreenOn() {
            ScreenUtils.setAlwaysOn(this, false)
        }
    }

    fun init() {
        super.init(
                NavigatorMain::class.java,
                R.layout.activity_maps,
                R.menu.maps_menu,
                true
        )
    }

    @INTERNAL_OPERATION
    override fun setup() {
        initUI()
        initMediaPlayers()
    }

    private fun initUI() {
        execute {
            wrappedActivity = getActivity("NavigatorGUI") as NavigatorMain
            wrappedActivity?.loadMap(this)
        }
        initFAB()
    }

    private fun initFAB() {
        execute {
            navigationSupportFAB = findUIElement(R.id.fab_spot) as FloatingActionButton
            navigationSupportFAB.setOnClickListener {
                if (isSupportEnable) {
                    stopSpottingService()
                } else {
                    startSpottingService()
                }
                isSupportEnable = !isSupportEnable
                beginExternalSession()
                signal("isSupportEnable", isSupportEnable)
                endExternalSession(true)
            }
        }
        signal("isSupportEnable", isSupportEnable)
    }

    private fun startSpottingService() {
        Toast.makeText(applicationContext, applicationContext.getString(R.string.road_scan_started), Toast.LENGTH_SHORT).show()
        navigationSupportFAB.backgroundTintList = ColorStateList.valueOf(applicationContext.getColor(R.color.secondaryColor))
        navigationSupportFAB.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_pan_tool_white_24dp))
        wrappedActivity?.keepScreenOn()
    }

    private fun stopSpottingService() {
        Toast.makeText(applicationContext, applicationContext.getString(R.string.road_scan_stopped), Toast.LENGTH_SHORT).show()
        navigationSupportFAB.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(applicationContext, R.color.primaryColor))
        navigationSupportFAB.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_time_to_leave_white_24dp))
        wrappedActivity?.disableKeepScreenOn()
    }

    private fun updateCameraLocation(
            location: LatLng,
            bearing: Float,
            zoom: Float = ZOOM,
            tilt: Float = TILT
    ) {
        if (!mapReady) return
        val cameraUpdate = CameraPosition.Builder()
                .target(location)
                .zoom(zoom)
                .tilt(tilt)
                .bearing(bearing)
                .build()
        Log.d(javaClass.name, "Update camera location: $location, bearing: $bearing")
        execute { mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraUpdate)) }
    }

    private fun updateRotation(newTargetRotation: Float) {
        if (!mapReady) return
        if (MapUtils.rotationGap(
                        currentCarTargetRotation,
                        newTargetRotation
                ) > DELTA_TRIGGER_ROTATION
        ) {
            Log.d(
                    javaClass.name,
                    "Rotating car marker from $currentCarTargetRotation to $newTargetRotation"
            )
            currentCarTargetRotation = newTargetRotation
            execute { carMarker!!.rotation = newTargetRotation }
        }
    }

    private fun drawLeg(legs: List<Leg>) {
        legs.forEach { leg ->
            val polyline = PolylineOptions()
                    .addAll(listOf(leg.from.coordinates.toLatLng(), leg.to.coordinates.toLatLng()))
                    .color(Quality.fromValue(leg.quality)!!.color)
                    .startCap(RoundCap())
                    .endCap(RoundCap())
            execute {
                mMap.addPolyline(polyline)
            }
        }
    }

    private fun drawObstacle(obstacles: List<Obstacle>) {
        obstacles.forEach {
            val drawable = when (it.obstacleType) {
                ObstacleType.NOTHING -> null
                ObstacleType.POTHOLE -> R.drawable.ic_pothole_marker
                ObstacleType.SPEED_BUMP -> R.drawable.ic_speedbump_marker
            }
            if (drawable != null) {
                val circleDrawable = ContextCompat.getDrawable(applicationContext, drawable)
                val markerIcon = DrawableUtils.getMarkerIconFromDrawable(circleDrawable!!)
                val marker = MarkerOptions().position(it.coordinates.toLatLng())
                        .anchor(0.5f, 1f).icon(markerIcon)
                execute {
                    mMap.addMarker(marker)
                }
            }
        }
    }

    private fun updateCarLocation(position: LatLng) {
        if (!mapReady) return
        val handler = Handler(Looper.getMainLooper())
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
                    finalScreenPosition.y < SCREEN_PADDING || finalScreenPosition.y > screenSize.y - SCREEN_PADDING
            ) {
                updateCameraLocation(position, newBearing)
            }
            currentCameraBearing = newBearing
        }

        stopPreviousInterpolation.set(true)

        fun interpolate(stopInterpolation: AtomicBoolean): Runnable {
            return object : Runnable {
                override fun run() {
                    val elapsed = SystemClock.uptimeMillis() - start
                    val t: Float =
                            interpolator.getInterpolation(elapsed.toFloat() / INTERPOLATE_DURATION)

                    val lng: Double = t * position.longitude + (1 - t) * startLatLng.longitude
                    val lat: Double = t * position.latitude + (1 - t) * startLatLng.latitude
                    val pos = LatLng(lat, lng)
                    carMarker!!.position = pos

                    if (t < 1.0 && !stopInterpolation.get()) {
                        handler.postDelayed(this, INTERPOLATE_INTERVAL)
                    }
                }
            }
        }

        stopPreviousInterpolation = AtomicBoolean(false)
        handler.post(interpolate(stopPreviousInterpolation))
    }

    @OPERATION
    fun updatePosition(gpsInfo: GpsLocation) {
        Log.v("Agent", "NavigatorAgent send new position [$gpsInfo]")
        if (mapReady) {
            val gpsInformation = gpsInfo
            val latLng = LatLng(gpsInformation.latitude, gpsInformation.longitude)
            if (carMarker == null) {
                val circle =
                        ContextCompat.getDrawable(applicationContext, R.drawable.ic_up_arrow_circle)
                val markerIcon = DrawableUtils.getMarkerIconFromDrawable(circle!!)
                execute {
                    carMarker = mMap.addMarker(
                            MarkerOptions()
                                    .position(latLng)
                                    .anchor(0.5f, 0.5f)
                                    .rotation(0f)
                                    .flat(true)
                                    .icon(markerIcon)
                    )
                }
            } else {
                execute { updateCarLocation(latLng) }
            }
            updateCameraLocation(latLng, 0f)
            execute {
                beginExternalSession()
                updateObsProperty("actualRadius", MapUtils.getVisibleRadius(mMap.projection.visibleRegion))
                endExternalSession(true)
            }
        }
    }

    @OPERATION
    fun updateQualities(qualities: List<Leg>) {
        drawLeg(qualities)
    }

    @OPERATION
    fun updateObstacles(obstacles: List<Obstacle>) {
        drawObstacle(obstacles)
    }

    @OPERATION
    fun emitAlarm(obstacleType: ObstacleType) {
        if (obstacleType == ObstacleType.POTHOLE) {
            holeMediaPlayer?.start()
        } else if (obstacleType == ObstacleType.SPEED_BUMP) {
            speedBumpMediaPlayer?.start()
        }
    }

    @OPERATION
    fun checkAlarm(obstacles: List<Obstacle>, gpsInfo: GpsLocation) {
        obstacles.forEach {
            val latLng = LatLng(gpsInfo.latitude, gpsInfo.longitude)
            val speed = gpsInfo.speed
            val distanceBetweenCarAndObstacle = MapUtils.distBetween(latLng, it.coordinates.toLatLng())
            if ((speed!! > 0.0f && distanceBetweenCarAndObstacle / speed < OBSTACLE_ALARM_THRESHOLD_IN_SECONDS
                            || distanceBetweenCarAndObstacle < OBSTACLE_ALARM_THRESHOLD_IN_METERS) && !alreadySignaled.contains(it)) {
                alreadySignaled.add(it)
                signal("alarmNeeded", it.obstacleType)
            }
        }
    }

    private fun initMediaPlayers() {
        holeMediaPlayer = MediaPlayer.create(applicationContext, R.raw.it_pothole_female)
        speedBumpMediaPlayer = MediaPlayer.create(applicationContext, R.raw.it_speedbump_female)
    }

    private fun releaseMediaPlayers() {
        holeMediaPlayer?.release()
        speedBumpMediaPlayer?.release()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NONE
        mMap.addTileOverlay(
                TileOverlayOptions().tileProvider(CustomTileProvider()).zIndex(-1f).fadeIn(true)
        )
        mapReady = true
        defineObsProperty("actualRadius", MapUtils.getVisibleRadius(mMap.projection.visibleRegion))
    }

    companion object {
        private const val DELTA_TRIGGER_ROTATION: Int = 15
        private const val ZOOM: Float = 18f
        private const val TILT: Float = 45f
        private const val OBSTACLE_ALARM_THRESHOLD_IN_SECONDS = 2
        private const val OBSTACLE_ALARM_THRESHOLD_IN_METERS = 50
        private const val SCREEN_PADDING = 500
        private const val INTERPOLATE_INTERVAL = 16L
        private const val INTERPOLATE_DURATION = 1000
        private const val CAMERA_BEARING_THRESHOLD = 15f
    }

}