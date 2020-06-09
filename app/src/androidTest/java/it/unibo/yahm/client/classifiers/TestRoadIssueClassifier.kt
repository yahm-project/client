package it.unibo.yahm.client.classifiers

import androidx.test.platform.app.InstrumentationRegistry
import it.unibo.yahm.client.entities.ObstacleType
import org.junit.Assert.assertEquals
import org.junit.Test


class TestRoadIssueClassifier {

    @Test
    fun testClassifier() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val classifier = RoadIssueClassifier(appContext)
        val inputSize = classifier.inputShape.reduce { a, b -> a * b }
        val zeroData = FloatArray(inputSize)

        assertEquals(ObstacleType.NOTHING, classifier.classify(zeroData))
    }
}
