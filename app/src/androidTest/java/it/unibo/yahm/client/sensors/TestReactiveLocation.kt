package it.unibo.yahm.client.sensors

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test


class TestReactiveLocation {

    @Test
    fun testReactiveLocation() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val reactiveLocation = ReactiveLocation(appContext, 0.0f, 100)
        reactiveLocation.observe()
        reactiveLocation.dispose()
    }

}