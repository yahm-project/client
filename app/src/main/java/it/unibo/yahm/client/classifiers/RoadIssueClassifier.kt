package it.unibo.yahm.client.classifiers

import android.content.Context
import android.util.Log
import it.unibo.yahm.client.entities.ObstacleType
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.IOException



data class RoadIssueClassifier(val applicationContext: Context) {

    companion object {
        private const val MODEL_FILE_NAME = "pothole-net.tflite"
    }


    fun classify(input: List<List<List<Float>>>): ObstacleType? {

        val probabilityBuffer =
            TensorBuffer.createFixedSize(intArrayOf(1, 3), DataType.UINT8)

        val tflite: Interpreter? = try{
            //val tfliteModel = FileUtil.loadMappedFile(applicationContext, "mobilenet_v1_1.0_224_quant.tflite");
            Interpreter(File(MODEL_FILE_NAME)!!)
        } catch (e: IOException){
            Log.e("tfliteSupport", "Error reading model", e);
            null
        }

        // Running inference
        tflite?.run(input, probabilityBuffer.buffer)

        val obstacleTypeIndex = probabilityBuffer.buffer.asFloatBuffer().array().withIndex().maxBy { it.value }?.index

        return if(obstacleTypeIndex != null && obstacleTypeIndex!! != 0) {
            ObstacleType.values()[obstacleTypeIndex!!]
        } else {
            null
        }
    }

}