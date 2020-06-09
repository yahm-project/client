package it.unibo.yahm.client.classifiers

import io.reactivex.rxjava3.functions.Function
import it.unibo.yahm.client.entities.Coordinate
import it.unibo.yahm.client.entities.Quality
import it.unibo.yahm.client.sensors.CombinedValues
import it.unibo.yahm.client.sensors.StretchQuality
import kotlin.math.abs


class RoadQualityClassifier : Function<CombinedValues, StretchQuality> {

    override fun apply(t: CombinedValues): StretchQuality {
        val zValues = t.accelerationValues.map { it.z }
        var sumOfDifferences = 0.0
        val l = t.location!!
        for (index in 0..zValues.size - 2) {
            sumOfDifferences += abs(zValues[index] - zValues[index + 1])
        }
        return StretchQuality(
            Coordinate(l.latitude, l.longitude), l.time,
            l.accuracy?.toDouble() ?: DEFAULT_ACCURACY, chooseQuality(sumOfDifferences / zValues.size)
        )
    }

    private fun chooseQuality(avg: Double): Quality {
        return if (avg <= PERFECT_THRESHOLD) {
            Quality.PERFECT
        } else if (avg > PERFECT_THRESHOLD && avg <= GOOD_THRESHOLD) {
            Quality.GOOD
        } else if (avg > GOOD_THRESHOLD && avg <= MEDIUM_THRESHOLD) {
            Quality.OK
        } else if (avg > MEDIUM_THRESHOLD && avg <= BAD_THRESHOLD) {
            Quality.BAD
        } else {
            Quality.VERY_BAD
        }
    }

    companion object {
        const val PERFECT_THRESHOLD = 1.2
        const val GOOD_THRESHOLD = 2.2
        const val MEDIUM_THRESHOLD = 3.2
        const val BAD_THRESHOLD = 4.2
        const val DEFAULT_ACCURACY = 100.0
    }

}
