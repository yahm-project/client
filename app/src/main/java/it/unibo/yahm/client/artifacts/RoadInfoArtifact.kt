package it.unibo.yahm.client.artifacts

import androidx.collection.LruCache
import cartago.OPERATION
import com.google.android.gms.maps.model.LatLng
import it.unibo.pslab.jaca_android.core.ServiceArtifact
import it.unibo.yahm.client.SpotholeService
import it.unibo.yahm.client.entities.Leg
import it.unibo.yahm.client.entities.Obstacle
import it.unibo.yahm.client.sensors.GpsLocation
import it.unibo.yahm.client.services.RetrofitService

class RoadInfoArtifact : ServiceArtifact() {
    private val legs: MutableList<Leg> = mutableListOf()
    private var obstacles: MutableList<Obstacle> = mutableListOf()
    private var legsCache: LruCache<Leg, Long> = LruCache(MAX_CACHE_SIZE)
    private var obstaclesCache: LruCache<Obstacle, Long> = LruCache(MAX_CACHE_SIZE)
    private lateinit var backendService: SpotholeService

    fun init() {
        backendService = RetrofitService(applicationContext).spotholeService
    }

    private fun fetchNewData(location: LatLng, radius: Double) {
        legs.clear()
        backendService
                .loadEvaluationsFromUserPerspective(location.latitude, location.longitude, radius.toFloat())
                .blockingForEach { legs ->
                    legs.forEach { leg ->
                        if (legsCache.put(leg, System.currentTimeMillis()) == null) {
                            this.legs.add(leg)
                        }
                        leg.obstacles.forEach { (obstacleType, obsCoordinateList) ->
                            obsCoordinateList.forEach {
                                val obstacle = Obstacle(it, obstacleType)
                                if (obstaclesCache.put(obstacle, System.currentTimeMillis()) == null) {
                                    obstacles.add(obstacle)
                                }
                            }
                        }
                    }
                }

    }

    @OPERATION
    fun updateConditions(location: GpsLocation, radius: Double) {
        fetchNewData(LatLng(location.latitude, location.longitude), (2 * radius).coerceAtMost(MAX_RADIUS_METERS))
        if (getObsProperty("obstacles") != null && getObsProperty("qualities") != null) {
            updateObsProperty("obstacles", obstacles)
            updateObsProperty("qualities", legs)
        } else {
            defineObsProperty("obstacles", obstacles)
            defineObsProperty("qualities", legs)
        }

    }

    companion object {
        private const val MAX_CACHE_SIZE = 1024 * 1024
        private const val MAX_RADIUS_METERS = 2000.0
    }
}