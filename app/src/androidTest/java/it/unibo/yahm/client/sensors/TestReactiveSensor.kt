package it.unibo.yahm.client.sensors

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test


class TestReactiveSensor {

    @Test
    fun testReactiveSensor() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val reactiveSensor = ReactiveSensor(appContext)

        SensorType.values().forEach { type ->
            val startTime = System.nanoTime()
            assertTrue(reactiveSensor.observer(type).take(100).all { it.timestamp >= startTime }.blockingGet())
            reactiveSensor.dispose(type)
        }
    }

}