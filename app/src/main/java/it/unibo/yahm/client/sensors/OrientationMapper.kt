package it.unibo.yahm.client.sensors

import android.hardware.SensorEvent
import android.hardware.SensorManager
import io.reactivex.rxjava3.functions.Function

class OrientationMapper : Function<SensorEvent, Float> {

    private val orientation = FloatArray(3)
    private val rMat = FloatArray(9)

    override fun apply(t: SensorEvent): Float {
        SensorManager.getRotationMatrixFromVector(rMat, t.values)
        // get the azimuth value (orientation[0]) in degree
        return ((Math.toDegrees(
            SensorManager.getOrientation(
                rMat,
                orientation
            )[0].toDouble()
        ) + 360) % 360).toFloat()
    }

}
