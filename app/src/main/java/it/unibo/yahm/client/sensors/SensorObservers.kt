package it.unibo.yahm.client.sensors

import android.hardware.SensorEvent
import android.location.Location
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import it.unibo.yahm.client.utils.FunctionUtils.median
import java.util.concurrent.TimeUnit
import kotlin.math.abs


class SensorObservers(reactiveLocation: ReactiveLocation, reactiveSensor: ReactiveSensor) {

    private val accelerometerObserver = reactiveSensor.observer(SensorType.ACCELEROMETER)
    private val gyroscopeObserver = reactiveSensor.observer(SensorType.GYROSCOPE)
    private val gpsObserver = reactiveLocation.observe()


    fun observeForStretchQualityProcessing(minStretchDistance: Double = 20.0):
            Observable<StretchQualityInput> {
        val subject = PublishSubject.create<StretchQualityInput>()
        val thread = Schedulers.newThread()

        var startLocation: Location? = null
        var lastLocation: Location? = null
        var distance = 0.0
        val accelerationEvents = mutableListOf<SensorEvent>()
        val gyroscopeEvents = mutableListOf<SensorEvent>()

        // aggregate by stretches length
        gpsObserver.subscribeOn(thread).subscribe {
            if (startLocation == null) {
                startLocation = it
            }
            if (lastLocation != null) {
                distance += lastLocation!!.distanceTo(it)
            }
            lastLocation = it

            if (distance > minStretchDistance) {
                subject.onNext(
                    StretchQualityInput(
                        startLocation!!, distance,
                        accelerationEvents.toList(), gyroscopeEvents.toList()
                    )
                )
                accelerationEvents.clear()
                gyroscopeEvents.clear()
                startLocation = null
                lastLocation = null
                distance = 0.0
            }
        }

        accelerometerObserver.subscribeOn(thread).subscribe {
            accelerationEvents.add(it)
        }
        gyroscopeObserver.subscribeOn(thread).subscribe {
            gyroscopeEvents.add(it)
        }

        return subject
    }

    // TODO: incompleted
    fun observeForObstacleProcessing(timeSpan: Long = 250, timeSkip: Long = 150) {
        val thread = Schedulers.newThread()
        val timedLocationSubscriber = TimedLocationSubscriber(gpsObserver)

        Observable.combineLatest<SensorEvent, SensorEvent, Pair<SensorEvent, SensorEvent>>(
            accelerometerObserver.subscribeOn(thread), gyroscopeObserver.subscribeOn(thread),
            BiFunction {accelerometerEvent, gyroscopeEvent -> Pair(accelerometerEvent, gyroscopeEvent)}
        ).buffer(timeSpan, timeSkip, TimeUnit.MILLISECONDS)
            .filter { it.isNotEmpty() }
            .map { }
            .subscribeOn(thread)
    }

    fun observeForSensorValues(
        accelerationAggregateFunction: (List<Acceleration>) -> Acceleration,
        angularVelocityAggregateFunction: (List<AngularVelocity>) -> AngularVelocity,
        timeSpan: Long = 20
    ): Observable<SensorValues> {
        val thread = Schedulers.newThread()
        val timedLocationSubscriber = TimedLocationSubscriber(gpsObserver)

        return Observable.combineLatest<SensorEvent, SensorEvent, Pair<SensorEvent, SensorEvent>>(
            accelerometerObserver.subscribeOn(thread),
            gyroscopeObserver.subscribeOn(thread),
            BiFunction { accelerometerEvent, gyroscopeEvent ->
                Pair(accelerometerEvent, gyroscopeEvent)
            }).filter { abs(it.first.timestamp - it.second.timestamp) < MAX_TIMESTAMP_DIFFERENCE }
            .buffer(timeSpan, TimeUnit.MILLISECONDS)
            .filter { it.isNotEmpty() }
            .map { pairs ->
                val accelerations = pairs.map { Acceleration.fromSensorEvent(it.first) }
                val angularVelocities = pairs.map { AngularVelocity.fromSensorEvent(it.second) }
                val averageTimestamp = ((pairs.map { it.first.timestamp }.median() +
                        pairs.map { it.second.timestamp }.median()) / 2)
                val location = timedLocationSubscriber.locationAt(averageTimestamp)
                SensorValues(accelerationAggregateFunction(accelerations),
                    angularVelocityAggregateFunction(angularVelocities),
                    averageTimestamp,
                    if (location != null) GpsLocation.fromLocation(location) else null)
            }.subscribeOn(thread)
    }

    companion object {
        private const val MAX_TIMESTAMP_DIFFERENCE = 10 * 1000 * 1000 // 10 millis
    }

}
