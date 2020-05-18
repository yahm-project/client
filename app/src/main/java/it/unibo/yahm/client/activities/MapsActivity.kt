package it.unibo.yahm.client.activities

import android.graphics.Paint
import android.graphics.Point
import android.location.Location
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
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import it.unibo.yahm.client.SpotholeService
import it.unibo.yahm.client.entities.Coordinate
import it.unibo.yahm.client.entities.Leg
import it.unibo.yahm.client.entities.ObstacleType
import it.unibo.yahm.client.entities.Quality
import it.unibo.yahm.client.sensors.ReactiveLocation
import it.unibo.yahm.client.sensors.ReactiveSensor
import it.unibo.yahm.client.utils.CustomTileProvider
import it.unibo.yahm.client.utils.DrawableUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.math.*
import it.unibo.yahm.R
import it.unibo.yahm.client.sensors.OrientationMapper
import it.unibo.yahm.client.sensors.SensorType

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
    private var drawedLegs: Map<Leg, Polyline> = emptyMap()
    private var drawedObstacles: Map<Coordinate, List<Marker>> = Collections.emptyMap()
    private var reactiveSensor: ReactiveSensor? = null
    private var reactiveLocation: ReactiveLocation? = null
    private var currentCameraBearing = BEARING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        reactiveSensor = ReactiveSensor(applicationContext)
        reactiveLocation = ReactiveLocation(applicationContext)



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
        val tileProvider: TileProvider = CustomTileProvider()
        val tileOverlay: TileOverlay = mMap.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider).zIndex(-99f).fadeIn(true))


        carMarker = addCarMarker(
            DEFAULT_LOCATION,
            BEARING
        )
        observeCarSensors()

        val baseUrl = applicationContext.resources.getString(R.string.spothole_service_development_baseurl)
        val retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
        val service = retrofit.create(SpotholeService::class.java)

        service.loadEvaluationsFromUserPerspective(43.9714437, 12.6030226, 10000.0).subscribeOn(Schedulers.single()).subscribe ({
            it.forEach { leg -> drawLeg(leg)}
        },{ Log.e("testError", it.toString())})
    }


    private fun drawLeg(leg: Leg) {
        runOnUiThread {
            val polyline = mMap.addPolyline(
                PolylineOptions().addAll(listOf(leg.from.coordinates.toLatLng(), leg.to.coordinates.toLatLng()))
                    .color(Quality.fromValue(leg.quality.roundToInt())!!.color.toArgb())
                    .startCap(RoundCap()).endCap(RoundCap())
            )
            drawedLegs += leg to polyline
        }
        leg.obstacles.forEach{(obsType, obsCoordinateList) -> obsCoordinateList.forEach{coordinate -> drawObstacle(obsType, coordinate)}}
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
            drawedObstacles += coordinate to listOf(obstacleMarker) + (drawedObstacles.getOrElse(coordinate, { -> listOf<Marker>()}))
        }
    }

    private fun updateCarLocation(position: Location) {
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

                if (t > 1.0 ) {
                    val latLng = LatLng(position.latitude, position.longitude)
                    updateCameraLocation(location = latLng, bearing = currentCameraBearing)
                    runOnUiThread { carMarker!!.position = latLng}
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

    private fun rotationGap(first: Float, second: Float): Float {
        val rotationDelta: Float = (abs(first - second) % 360.0f)
        return if (rotationDelta > 180.0f) (360.0f - rotationDelta) else rotationDelta
    }

    /*
        La mappa deve adattare il proprio orientamento facendo in modo che il marker sia sempre rivolto verso l'alto,
        questo richiede che la mappa e la camera ruotino in sincronia non appena la rotazione dell'auto superi un certo delta rispetto ad essi.
    */
    private var isRotating = false
    private var currentCarTargetRotation =
        BEARING
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

        reactiveLocation!!.observe().observeOn(AndroidSchedulers.mainThread())
            .subscribe { updateCarLocation(it)
         }
        reactiveSensor!!.observer(SensorType.ROTATION_VECTOR)
            .observeOn(AndroidSchedulers.mainThread()).map(OrientationMapper()).subscribe {
            updateRotation(it);
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
