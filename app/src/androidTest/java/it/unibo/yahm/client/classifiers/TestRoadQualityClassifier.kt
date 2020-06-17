package it.unibo.yahm.client.classifiers

import it.unibo.yahm.client.entities.Quality
import it.unibo.yahm.client.sensors.Acceleration
import it.unibo.yahm.client.sensors.AngularVelocity
import it.unibo.yahm.client.sensors.CombinedValues
import it.unibo.yahm.client.sensors.GpsLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class TestRoadQualityClassifier {

    @Test
    fun testClassifier() {
        assertEquals(Quality.PERFECT, testClassifierFunction({ Acceleration(0.0, 0.0, 0.0) }))
        assertEquals(Quality.VERY_BAD, testClassifierFunction({ Acceleration(0.0, 0.0, Math.random() * 100) }))
    }

    private fun testClassifierFunction(accGenerator: () -> Acceleration, n: Int = 100): Quality {
        val now = System.currentTimeMillis()
        val zeroVelAng = AngularVelocity(0.0, 0.0, 0.0)
        val location = GpsLocation(10.0, 20.0, 30.0f, null, now)
        val values = CombinedValues(
            List(n) { accGenerator() },
            List(n) { zeroVelAng },
            location,
            10.0,
            System.currentTimeMillis()
        )

        val result = RoadQualityClassifier().apply(values)

        assertEquals(location.latitude, result.position.latitude, 0.001)
        assertEquals(location.longitude, result.position.longitude, 0.001)
        assertEquals(location.accuracy!!.toDouble(), result.radius, 0.001)
        assertEquals(now, result.timestamp)

        return result.quality
    }

}
