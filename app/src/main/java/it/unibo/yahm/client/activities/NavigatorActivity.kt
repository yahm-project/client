package it.unibo.yahm.client.activities

import android.graphics.Point
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cartago.INTERNAL_OPERATION
import cartago.OPERATION
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import it.unibo.pslab.jaca_android.core.ActivityArtifact
import it.unibo.pslab.jaca_android.core.JaCaBaseActivity
import it.unibo.yahm.R
import it.unibo.yahm.client.SpotholeService
import it.unibo.yahm.client.entities.Obstacle
import it.unibo.yahm.client.entities.ObstacleType
import it.unibo.yahm.client.entities.Quality
import it.unibo.yahm.client.sensors.GpsLocation
import it.unibo.yahm.client.services.RetrofitService
import it.unibo.yahm.client.utils.CustomTileProvider
import it.unibo.yahm.client.utils.DrawableUtils
import it.unibo.yahm.client.utils.MapUtils
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs


class NavigatorActivity : ActivityArtifact(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var holeMediaPlayer: MediaPlayer? = null
    private var speedBumpMediaPlayer: MediaPlayer? = null
    private lateinit var backendService: SpotholeService
    private var mapReady = false
    private var carMarker: Marker? = null
    private var currentCarTargetRotation = 0f
    private var updateRotationWithSensors = false
    private var stopPreviousInterpolation = AtomicBoolean(false)
    private var currentCameraBearing = 0f
    private val screenSize = Point()

    class NavigatorMain : JaCaBaseActivity() {
        fun loadMap(callback: OnMapReadyCallback) {
            (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(
                callback
            )
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
    }

    private fun initUI() {
        execute {
            val t: NavigatorMain = getActivity("NavigatorGUI") as NavigatorMain
            t.loadMap(this)
        }
    }

    private fun updateCameraLocation(
        location: LatLng,
        bearing: Float,
        zoom: Float = NavigatorActivity.ZOOM,
        tilt: Float = NavigatorActivity.TILT
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
    fun updateQualities(qualities: List<Quality>) {
        Log.v("Agent", "NavigatorAgent send new qualities [$qualities]")
    }

    @OPERATION
    fun updateObstacles(obstacles: List<Obstacle>) {
        Log.v("Agent", "NavigatorAgent] send new obstacles [$obstacles]")
    }

    @OPERATION
    fun emitAlarm(obstacleType: ObstacleType) {
        Log.v("Agent", "NavigatorAgent emit alarm for [$obstacleType]")
    }

    private fun initMediaPlayers() {
        holeMediaPlayer = MediaPlayer.create(applicationContext, R.raw.it_pothole_female)
        speedBumpMediaPlayer = MediaPlayer.create(applicationContext, R.raw.it_speedbump_female)
    }

    private fun initBackendService() {
        backendService = RetrofitService(applicationContext).spotholeService
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
        private const val MAX_RADIUS_METERS = 2000f //won't ask for disease further away than that
        private const val DEFAULT_RADIUS_METERS = MAX_RADIUS_METERS / 10
        private const val OBSTACLE_ALARM_THRESHOLD_IN_SECONDS = 3
        private const val OBSTACLE_ALARM_THRESHOLD_IN_METERS = 150
        private const val SCREEN_PADDING = 500
        private const val INTERPOLATE_INTERVAL = 16L
        private const val INTERPOLATE_DURATION = 1000
        private const val CAMERA_BEARING_THRESHOLD = 15f
        private const val INITIAL_GPS_ACCURACY_THRESHOLD = 50f
        private const val GPS_ACCURACY_THRESHOLD = 10f
    }

}