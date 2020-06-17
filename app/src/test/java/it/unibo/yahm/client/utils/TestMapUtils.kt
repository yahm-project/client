package it.unibo.yahm.client.utils

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TestMapUtils {

    @Test
    fun testDistanceBetween() {
        val a = LatLng(44.136352, 12.242244)
        val b = LatLng(41.891930,12.511330)
        val distance = 250471.0

        assertTrue(abs(MapUtils.distBetween(a, b) - distance) < 1.0)
        assertTrue(abs(MapUtils.distBetween(a, b) - MapUtils.distBetween(b, a)) < 1.0)
    }

}