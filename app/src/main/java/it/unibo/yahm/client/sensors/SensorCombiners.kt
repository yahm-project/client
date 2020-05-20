package it.unibo.yahm.client.sensors

import android.hardware.SensorEvent
import android.location.Location
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import kotlin.math.abs


class SensorCombiners(reactiveLocation: ReactiveLocation, reactiveSensor: ReactiveSensor) {

    private val accelerometerObserver = reactiveSensor.observer(SensorType.LINEAR_ACCELERATION)
    private val gyroscopeObserver = reactiveSensor.observer(SensorType.GYROSCOPE)
    private val gpsObserver = reactiveLocation.observe()


    fun combineByStretchLength(minStretchLength: Double = 20.0): Observable<CombinedValues> {
        val subject = PublishSubject.create<CombinedValues>()
        val thread = Schedulers.newThread()

        var startLocation: Location? = null
        var lastLocation: Location? = null
        var length = 0.0
        val accelerationValues = mutableListOf<Acceleration>()
        val gyroscopeValues = mutableListOf<AngularVelocity>()

        // aggregate by stretches length
        gpsObserver.subscribeOn(thread).subscribe {
            if (startLocation == null) {
                startLocation = it
            }
            if (lastLocation != null) {
                length += lastLocation!!.distanceTo(it)
            }
            lastLocation = it

            if (length > minStretchLength) {
                subject.onNext(
                    CombinedValues(
                        accelerationValues.toList(), gyroscopeValues.toList(),
                        GpsLocation.fromLocation(startLocation!!),
                        length, startLocation!!.time
                    )
                )
                accelerationValues.clear()
                gyroscopeValues.clear()
                startLocation = null
                lastLocation = null
                length = 0.0
            }
        }

        accelerometerObserver.subscribeOn(thread).subscribe {
            accelerationValues.add(Acceleration.fromSensorEvent(it))
        }
        gyroscopeObserver.subscribeOn(thread).subscribe {
            gyroscopeValues.add(AngularVelocity.fromSensorEvent(it))
        }

        return subject
    }

    fun combineByTime(timeSpan: Long = 20, timeSkip: Long? = null): Observable<CombinedValues> {
        val thread = Schedulers.newThread()
        val timedLocationSubscriber = TimedLocationSubscriber(gpsObserver)

        return Observable.combineLatest<SensorEvent, SensorEvent, Pair<SensorEvent, SensorEvent>>(
            accelerometerObserver.subscribeOn(thread),
            gyroscopeObserver.subscribeOn(thread),
            BiFunction { accelerometerEvent, gyroscopeEvent ->
                Pair(accelerometerEvent, gyroscopeEvent)
            }).filter { abs(it.first.timestamp - it.second.timestamp) < MAX_TIMESTAMP_DIFFERENCE }
            .buffer(timeSpan, timeSkip ?: timeSpan, TimeUnit.MILLISECONDS)
            .filter { it.isNotEmpty() }
            .map { pairs ->
                val accelerationValues = pairs.map { Acceleration.fromSensorEvent(it.first) }
                val gyroscopeValues = pairs.map { AngularVelocity.fromSensorEvent(it.second) }
                val timestamp = System.currentTimeMillis()

                val location = timedLocationSubscriber.locationAt(timestamp)
                CombinedValues(
                    accelerationValues, gyroscopeValues,
                    if (location != null) GpsLocation.fromLocation(location) else null,
                    null, timestamp
                )
            }.subscribeOn(thread)
    }

    companion object {
        private const val MAX_TIMESTAMP_DIFFERENCE = 10 * 1000 * 1000 // 10 millis
    }

}
