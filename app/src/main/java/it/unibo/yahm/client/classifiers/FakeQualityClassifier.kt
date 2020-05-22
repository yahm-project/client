package it.unibo.yahm.client.classifiers

import io.reactivex.rxjava3.functions.Function
import it.unibo.yahm.client.entities.Coordinate
import it.unibo.yahm.client.entities.Quality
import it.unibo.yahm.client.sensors.CombinedValues
import it.unibo.yahm.client.sensors.StretchQuality


class FakeQualityClassifier : Function<CombinedValues, StretchQuality> {

    override fun apply(t: CombinedValues): StretchQuality {
        val l = t.location!!
        return StretchQuality(
            Coordinate(l.latitude, l.longitude), l.time,
            l.accuracy?.toDouble() ?: 100.0, Quality.random()
        )
    }

}
