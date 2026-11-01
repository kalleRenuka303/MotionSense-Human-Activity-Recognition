package com.motionsense.har.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.motionsense.har.data.model.ActivityType
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.math.max

data class ClassificationResult(
    val activityType: ActivityType,
    val confidence: Float,
    val probabilities: FloatArray,
    val isUncertain: Boolean,
    val inferenceTimeMs: Long
)

class MotionClassifier(private val context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var cnnLstmSession: OrtSession? = null
    private var cnn1dSession: OrtSession? = null

    companion object {
        private const val TAG = "MotionClassifier"
        const val MODEL_CNN_LSTM = "cnn_lstm"
        const val MODEL_1D_CNN = "cnn_1d"
        const val WINDOW_SIZE = 128
        const val CHANNELS = 9
    }

    init {
        loadModels()
    }

    private fun loadModels() {
        try {
            val cnnBytes = context.assets.open("models/cnn_1d.onnx").readBytes()
            cnn1dSession = env.createSession(cnnBytes, OrtSession.SessionOptions())
            Log.d(TAG, "1D CNN ONNX Model loaded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load 1D CNN ONNX model: ${e.message}", e)
        }

        try {
            val lstmBytes = context.assets.open("models/cnn_lstm.onnx").readBytes()
            cnnLstmSession = env.createSession(lstmBytes, OrtSession.SessionOptions())
            Log.d(TAG, "CNN-LSTM ONNX Model loaded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load CNN-LSTM ONNX model: ${e.message}", e)
        }
    }

    /**
     * Classifies a 128x9 window of sensor readings.
     * @param windowData 1D FloatArray of size 128 * 9 = 1152 elements (flattened row-major).
     * @param modelType "cnn_lstm" or "cnn_1d"
     * @param uncertaintyThreshold Confidence cutoff below which result is flagged uncertain.
     */
    fun classifyWindow(
        windowData: FloatArray,
        modelType: String = MODEL_CNN_LSTM,
        uncertaintyThreshold: Float = 0.55f
    ): ClassificationResult {
        val startTime = System.currentTimeMillis()
        val session = if (modelType == MODEL_1D_CNN) {
            cnn1dSession ?: cnnLstmSession
        } else {
            cnnLstmSession ?: cnn1dSession
        } ?: return fallbackResult()

        val tensorShape = longArrayOf(1, WINDOW_SIZE.toLong(), CHANNELS.toLong())
        val floatBuffer = FloatBuffer.wrap(windowData)

        try {
            val inputTensor = OnnxTensor.createTensor(env, floatBuffer, tensorShape)
            val results = session.run(mapOf("input" to inputTensor))
            val outputTensor = results[0].value as Array<FloatArray>
            val logits = outputTensor[0]

            val probabilities = softmax(logits)
            var maxIndex = 0
            var maxProb = 0f
            for (i in probabilities.indices) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }

            val isUncertain = maxProb < uncertaintyThreshold
            val activity = if (isUncertain) ActivityType.UNCERTAIN else ActivityType.fromIndex(maxIndex)
            val latency = System.currentTimeMillis() - startTime

            inputTensor.close()
            results.close()

            return ClassificationResult(
                activityType = activity,
                confidence = maxProb,
                probabilities = probabilities,
                isUncertain = isUncertain,
                inferenceTimeMs = latency
            )
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}", e)
            return fallbackResult()
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        var maxVal = Float.NEGATIVE_INFINITY
        for (v in logits) {
            if (v > maxVal) maxVal = v
        }
        var sum = 0f
        val expArray = FloatArray(logits.size)
        for (i in logits.indices) {
            val e = exp(logits[i] - maxVal)
            expArray[i] = e
            sum += e
        }
        val safeSum = if (sum > 0f) sum else 1f
        for (i in expArray.indices) {
            expArray[i] = expArray[i] / safeSum
        }
        return expArray
    }

    private fun fallbackResult(): ClassificationResult {
        return ClassificationResult(
            activityType = ActivityType.UNCERTAIN,
            confidence = 0f,
            probabilities = FloatArray(6) { 1f / 6f },
            isUncertain = true,
            inferenceTimeMs = 0L
        )
    }

    fun close() {
        try {
            cnn1dSession?.close()
            cnnLstmSession?.close()
            env.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ORT environment: ${e.message}")
        }
    }
}
