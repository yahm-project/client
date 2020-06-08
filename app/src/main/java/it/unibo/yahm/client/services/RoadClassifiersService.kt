package it.unibo.yahm.client.services

import android.content.Context
import android.util.Log
import android.widget.Toast
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import it.unibo.yahm.R
import it.unibo.yahm.client.SpotholeService
import it.unibo.yahm.client.classifiers.FakeQualityClassifier
import it.unibo.yahm.client.classifiers.RoadIssueClassifier
import it.unibo.yahm.client.classifiers.RoadQualityClassifier
import it.unibo.yahm.client.entities.Coordinate
import it.unibo.yahm.client.entities.Evaluations
import it.unibo.yahm.client.entities.Obstacle
import it.unibo.yahm.client.entities.ObstacleType
import it.unibo.yahm.client.sensors.ReactiveLocation
import it.unibo.yahm.client.sensors.ReactiveSensor
import it.unibo.yahm.client.sensors.SensorCombiners
import it.unibo.yahm.client.utils.FunctionUtils.median


class RoadClassifiersService(
    private val context: Context,
    reactiveSensor: ReactiveSensor,
    reactiveLocation: ReactiveLocation,
    private val spotholeService: SpotholeService
) {

    private val roadIssueClassifier = RoadIssueClassifier(context)
    private var roadIssueDisposable: Disposable? = null
    private var roadQualityDisposable: Disposable? = null
    private var scheduler: Scheduler? = null
    private val sensorCombiners = SensorCombiners(reactiveLocation, reactiveSensor)


    fun startService() {
        val obstacles: MutableList<Obstacle> = mutableListOf()
        scheduler = Schedulers.newThread()

        Log.d(javaClass.name, "Starting service..")

        roadIssueDisposable = sensorCombiners.combineByTime(SENSING_INTERVAL)
            .observeOn(scheduler)
            .filter {  it.location?.accuracy != null && it.location.accuracy < 10f}
            .buffer(WINDOW_LENGTH, (WINDOW_LENGTH * WINDOW_OVERLAP_PERCENTAGE).toInt())
            .map { values ->
                val inputBuffer = FloatArray(WINDOW_LENGTH * FEATURES_COUNT)

                values.forEachIndexed { i, cv ->
                    inputBuffer[i * FEATURES_COUNT] =
                        cv.accelerationValues.map { it.x }.median().toFloat()
                    inputBuffer[i * FEATURES_COUNT + 1] =
                        cv.accelerationValues.map { it.y }.median().toFloat()
                    inputBuffer[i * FEATURES_COUNT + 2] =
                        cv.accelerationValues.map { it.z }.median().toFloat()
                    inputBuffer[i * FEATURES_COUNT + 3] =
                        cv.gyroscopeValues.map { it.x }.median().toFloat()
                    inputBuffer[i * FEATURES_COUNT + 4] =
                        cv.gyroscopeValues.map { it.y }.median().toFloat()
                    inputBuffer[i * FEATURES_COUNT + 5] =
                        cv.gyroscopeValues.map { it.z }.median().toFloat()
                }
                val location = values.first { it.location != null }.location!!
                Obstacle(
                    Coordinate(location.latitude, location.longitude),
                    roadIssueClassifier.classify(inputBuffer)
                )
            }.filter { it.obstacleType != ObstacleType.NOTHING }
            .subscribeOn(scheduler)
            .subscribe ({
                obstacles.add(it)
            }, {
                Log.e("RoadClassifierService", "Failed to classify road obstacles")
                it.printStackTrace()
            })

        roadQualityDisposable = sensorCombiners.combineByStretchLength(MIN_STRETCH_LENGTH)
            .observeOn(scheduler)
            .map(RoadQualityClassifier())
            .buffer(QUALITY_BUFFER_SIZE, QUALITY_BUFFER_SIZE - 1)
            .flatMap { buf ->
                Log.d(javaClass.name, "Sending to server $QUALITY_BUFFER_SIZE legs..")
                spotholeService.sendEvaluations(
                    Evaluations(
                        buf.map { it.position },
                        buf.map { it.timestamp / 1000 }, // timestamp must be in seconds
                        buf.map { it.radius },
                        buf.take(QUALITY_BUFFER_SIZE - 1).map { it.quality },
                        obstacles
                    )
                ).retry()
            }
            .subscribe {
                Log.v(javaClass.name, "Request done")
            }
    }

    fun stopService() {
        Log.d(javaClass.name, "Stopping service..")

        scheduler?.shutdown()
        roadIssueDisposable?.dispose()
        roadQualityDisposable?.dispose()
    }

    companion object {
        private const val WINDOW_LENGTH = 128
        private const val WINDOW_OVERLAP_PERCENTAGE = 0.05
        private const val FEATURES_COUNT = 6
        private const val SENSING_INTERVAL: Long = 20
        private const val QUALITY_BUFFER_SIZE = 20
        private const val MIN_STRETCH_LENGTH = 20.0
    }

}
