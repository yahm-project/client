package it.unibo.yahm.client.training

import io.reactivex.rxjava3.core.Observable
import it.unibo.yahm.client.entities.Coordinate
import it.unibo.yahm.client.entities.Quality
import it.unibo.yahm.client.sensors.StretchQualityInput
import it.unibo.yahm.client.sensors.StretchQualityOutput


object FakeQualityClassifier {

    fun process(observable: Observable<StretchQualityInput>): Observable<StretchQualityOutput> =
        observable.map { StretchQualityOutput(
            Coordinate(it.location.latitude, it.location.longitude),
            it.location.time,
            if (it.location.hasAccuracy()) it.location.accuracy.toDouble() else 100.0,
            Quality.random()
        ) }

}
