package it.unibo.yahm.client.sensors

import android.location.Location
import android.os.Handler
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Assert.assertEquals
import org.junit.Test


class TestTimedLocationSubscriber {

    @Test
    fun testRetrieveLocationAt() {
        val subject = PublishSubject.create<Location>()
        val timedLocationSubscriber = TimedLocationSubscriber(subject)

        for (i in 99 downTo 0) {
            val l = Location("test")
            l.time = i.toLong()
            l.latitude = i.toDouble()
            l.longitude = i.toDouble()
            subject.onNext(l)
        }

        Handler().postDelayed({
            for (i in 0..99) {
                assertEquals(i.toDouble(), timedLocationSubscriber.locationAt(i.toLong())!!.longitude, 0.001)
                assertEquals(i.toDouble(), timedLocationSubscriber.locationAt(i.toLong())!!.latitude, 0.001)
                assertEquals(i.toLong(), timedLocationSubscriber.locationAt(i.toLong())!!.time)
            }

            timedLocationSubscriber.dispose()
        }, 1000)
    }
}
