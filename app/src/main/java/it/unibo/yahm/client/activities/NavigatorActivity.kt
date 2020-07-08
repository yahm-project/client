package it.unibo.yahm.client.activities

import android.util.Log
import cartago.INTERNAL_OPERATION
import cartago.OPERATION
import com.google.android.gms.maps.model.LatLng
import it.unibo.pslab.jaca_android.core.ActivityArtifact
import it.unibo.pslab.jaca_android.core.JaCaBaseActivity
import it.unibo.yahm.R
import it.unibo.yahm.client.entities.Obstacle
import it.unibo.yahm.client.entities.ObstacleType
import it.unibo.yahm.client.entities.Quality
import it.unibo.yahm.client.sensors.GpsLocation
import java.util.*

class NavigatorActivity : ActivityArtifact() {
    class NavigatorMain : JaCaBaseActivity(){}
    fun init() {
        super.init(
            NavigatorMain::class.java,
            R.layout.activity_maps,
            true
        )
    }

    @INTERNAL_OPERATION
    override fun setup() {
        initUI()
    }

    private fun initUI() {

    }

    @OPERATION
    fun updatePosition(gpsInfo: Optional<GpsLocation>){
        execute { Log.v("Agent","NavigatorAgent send new position [$gpsInfo]") }
    }
    @OPERATION
    fun updateQualities(qualities: List<Quality>){
        execute { Log.v("Agent","NavigatorAgent send new qualities [$qualities]") }
    }
    @OPERATION
    fun updateObstacles(obstacles: List<Obstacle>){
        execute { Log.v("Agent","NavigatorAgent] send new obstacles [$obstacles]") }
    }
    @OPERATION
    fun emitAlarm(obstacleType: ObstacleType){
        execute { Log.v("Agent","NavigatorAgent emit alarm for [$obstacleType]") }
    }
}