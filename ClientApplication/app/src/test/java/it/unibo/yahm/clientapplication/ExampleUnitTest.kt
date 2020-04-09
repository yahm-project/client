package it.unibo.yahm.clientapplication

import io.reactivex.rxjava3.core.Observable
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun bufferObserver() {
        val inputObserver = Observable.fromIterable(listOf(1,2,3,4,5,6))
        val bufferedSubject = (BufferedSubject<Int> { currentBuffer -> (currentBuffer.elementAt(currentBuffer.size-1) - currentBuffer.elementAt(0)) == 1 }).buffer(inputObserver)
        bufferedSubject.test().assertValueSequence(listOf(mutableListOf(1,2),mutableListOf(3,4),mutableListOf(5,6)))
    }
}
