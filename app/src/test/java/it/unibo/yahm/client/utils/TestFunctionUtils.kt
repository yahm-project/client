package it.unibo.yahm.client.utils

import it.unibo.yahm.client.utils.FunctionUtils.median
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class TestFunctionUtils {

    @Test
    fun testMedian() {
        assertEquals(1, listOf(-2, 0, 0, 1, 2, 5).median())
        assertEquals(1, listOf(1, 5, -2, 0, 0, 2).median())
        assertEquals(2, listOf(1, 2, 3).median())
        try {
            emptyList<Int>().median()
        } catch (e: NoSuchElementException) {
            // pass
        } catch (e: Exception) {
            fail()
        }
    }

}
