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
    val rawActivityType: ActivityType,
    val confidence: Float,
    val rawConfidence: Float,
    val logits: FloatArray,
    val probabilities: FloatArray,
    val isUncertain: Boolean,
    val inferenceTimeMs: Long,
    val modelUsed: String
)

class MotionClassifier(private val context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var cnnLstmSession: OrtSession? = null
    private var cnn1dSession: OrtSession? = null

    // Rolling history for temporal smoothing (3 windows = ~3.84 seconds)
    private val predictionHistory = ArrayList<ActivityType>(3)

    companion object {
        private const val TAG = "MotionClassifier"
        const val MODEL_1D_CNN = "cnn_1d"
        const val MODEL_CNN_LSTM = "cnn_lstm"
        const val WINDOW_SIZE = 128
        const val CHANNELS = 9
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.50f
        const val SMOOTHING_WINDOW_SIZE = 3
    }

    init {
        loadModels()
    }

    private fun loadModels() {
        try {
            val cnnBytes = context.assets.open("models/cnn_1d.onnx").readBytes()
            cnn1dSession = env.createSession(cnnBytes, OrtSession.SessionOptions())
            Log.d(TAG, "1D CNN ONNX Model loaded successfully (93.99% Test Acc).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load 1D CNN ONNX model: ${e.message}", e)
        }

        try {
            val lstmBytes = context.assets.open("models/cnn_lstm.onnx").readBytes()
            cnnLstmSession = env.createSession(lstmBytes, OrtSession.SessionOptions())
            Log.d(TAG, "CNN-LSTM ONNX Model loaded successfully (90.74% Test Acc).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load CNN-LSTM ONNX model: ${e.message}", e)
        }
    }

    /**
     * Classifies a 128x9 window of sensor readings.
     * @param windowData 1D FloatArray of size 128 * 9 = 1152 elements (flattened row-major).
     * @param modelType "cnn_1d" (recommended) or "cnn_lstm"
     * @param uncertaintyThreshold Minimum confidence below which result is flagged UNCERTAIN.
     * @param enableSmoothing Whether to apply rolling majority voting across consecutive windows.
     */
    fun classifyWindow(
        windowData: FloatArray,
        modelType: String = MODEL_1D_CNN,
        uncertaintyThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
        enableSmoothing: Boolean = true
    ): ClassificationResult {
        val startTime = System.currentTimeMillis()
        val session = if (modelType == MODEL_1D_CNN) {
            cnn1dSession ?: cnnLstmSession
        } else {
            cnnLstmSession ?: cnn1dSession
        } ?: return fallbackResult(modelType)

        val tensorShape = longArrayOf(1, WINDOW_SIZE.toLong(), CHANNELS.toLong())
        val floatBuffer = FloatBuffer.wrap(windowData)

        try {
            val inputTensor = OnnxTensor.createTensor(env, floatBuffer, tensorShape)
            val results = session.run(mapOf("input" to inputTensor))
            val outputTensor = results[0].value as Array<FloatArray>
            val rawLogits = outputTensor[0]

            val probabilities = softmax(rawLogits)
            var maxIndex = 0
            var maxProb = 0f
            for (i in probabilities.indices) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }

            val isUncertain = maxProb < uncertaintyThreshold
            val rawActivity = if (isUncertain) ActivityType.UNCERTAIN else ActivityType.fromIndex(maxIndex)

            // Temporal Smoothing (Majority Voting)
            var smoothedActivity = rawActivity
            if (enableSmoothing && rawActivity != ActivityType.UNCERTAIN) {
                predictionHistory.add(rawActivity)
                if (predictionHistory.size > SMOOTHING_WINDOW_SIZE) {
                    predictionHistory.removeAt(0)
                }

                // Majority vote over recent history
                val counts = HashMap<ActivityType, Int>()
                for (act in predictionHistory) {
                    counts[act] = (counts[act] ?: 0) + 1
                }
                var maxCount = 0
                for ((act, count) in counts) {
                    if (count > maxCount) {
                        maxCount = count
                        smoothedActivity = act
                    }
                }
            }

            val latency = System.currentTimeMillis() - startTime
            inputTensor.close()
            results.close()

            return ClassificationResult(
                activityType = smoothedActivity,
                rawActivityType = rawActivity,
                confidence = maxProb,
                rawConfidence = maxProb,
                logits = rawLogits.clone(),
                probabilities = probabilities.clone(),
                isUncertain = isUncertain,
                inferenceTimeMs = latency,
                modelUsed = if (modelType == MODEL_1D_CNN) "1D CNN (93.99%)" else "CNN-LSTM (90.74%)"
            )

        } catch (e: Exception) {
            Log.e(TAG, "ONNX inference failed: ${e.message}", e)
            return fallbackResult(modelType)
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        var maxLogit = Float.NEGATIVE_INFINITY
        for (v in logits) {
            if (v > maxLogit) maxLogit = v
        }

        val exps = FloatArray(logits.size)
        var sumExp = 0f
        for (i in logits.indices) {
            exps[i] = exp(logits[i] - maxLogit)
            sumExp += exps[i]
        }

        val probs = FloatArray(logits.size)
        for (i in logits.indices) {
            probs[i] = if (sumExp > 0f) exps[i] / sumExp else 1f / logits.size
        }
        return probs
    }

    fun resetHistory() {
        predictionHistory.clear()
    }

    fun close() {
        try {
            cnn1dSession?.close()
            cnnLstmSession?.close()
            env.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ONNX sessions: ${e.message}", e)
        }
    }

    private fun fallbackResult(modelType: String): ClassificationResult {
        return ClassificationResult(
            activityType = ActivityType.UNCERTAIN,
            rawActivityType = ActivityType.UNCERTAIN,
            confidence = 0f,
            rawConfidence = 0f,
            logits = FloatArray(6),
            probabilities = FloatArray(6) { 1f / 6f },
            isUncertain = true,
            inferenceTimeMs = 0L,
            modelUsed = modelType
        )
    }
}
