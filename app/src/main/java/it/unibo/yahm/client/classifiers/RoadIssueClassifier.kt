package it.unibo.yahm.client.classifiers

import android.content.Context
import android.util.Log
import it.unibo.yahm.client.entities.ObstacleType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class RoadIssueClassifier(context: Context) {

    private val interpreter: Interpreter
    private val inputBuffer: TensorBuffer
    private val outputBuffer: TensorBuffer
    private val labels: List<String>
    private val probabilityProcessor: TensorProcessor

    val inputShape: IntArray

    init {
        val options = Interpreter.Options()
        options.setNumThreads(1)

        val tfliteModel = FileUtil.loadMappedFile(context, MODEL_FILENAME)
        interpreter = Interpreter(tfliteModel, options)

        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)
        inputShape = inputTensor.shape()
        inputBuffer = TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType())
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())

        labels = FileUtil.loadLabels(context, LABELS_FILENAME)

        val op = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)
        probabilityProcessor = TensorProcessor.Builder().add(op).build()
    }

    fun classify(input: FloatArray): ObstacleType {
        inputBuffer.loadArray(input)
        interpreter.run(inputBuffer.buffer, outputBuffer.buffer.rewind())

        val labeledProbability =
            TensorLabel(labels, probabilityProcessor.process(outputBuffer)).mapWithFloatValue

        Log.d(javaClass.name, "Results: $labeledProbability")

        return ObstacleType.valueOf(labeledProbability.maxBy { it.value }!!.key)
    }

    companion object {
        private const val MODEL_FILENAME = "pothole-net.tflite"
        private const val LABELS_FILENAME = "labels.txt"
        private const val PROBABILITY_MEAN = 0.0f
        private const val PROBABILITY_STD = 1.0f
    }

}
