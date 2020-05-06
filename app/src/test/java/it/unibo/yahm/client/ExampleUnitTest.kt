package it.unibo.yahm.client

import io.reactivex.rxjava3.core.Observable
import it.unibo.yahm.client.sensors.BufferedSubject
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun bufferObserver() {
        val inputObserver = Observable.fromIterable(listOf(1, 2, 3, 4, 5, 6))

        (BufferedSubject<Int> { currentBuffer ->
            (currentBuffer.elementAt(currentBuffer.size - 1) - currentBuffer.elementAt(
                0
            )) == 1
        })
            .buffer(inputObserver)
            .test()
            .assertValueSequence(listOf(listOf(1, 2), listOf(3, 4), listOf(5, 6)))
    }
}
