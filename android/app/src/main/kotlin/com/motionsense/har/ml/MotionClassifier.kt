package com.motionsense.har.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.motionsense.har.data.model.ActivityType
import java.nio.FloatBuffer
import kotlin.math.exp

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
    private var binarySession: OrtSession? = null
    private var threeClassSession: OrtSession? = null
    private var cnn1dSession: OrtSession? = null
    private var cnnLstmSession: OrtSession? = null

    // Rolling history for temporal smoothing (3 windows = ~3.84 seconds)
    private val predictionHistory = ArrayList<ActivityType>(3)

    companion object {
        private const val TAG = "MotionClassifier"
        const val MODEL_BINARY_WALKING = "walking_binary"
        const val MODEL_3_CLASS = "three_class"
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
            val binaryBytes = context.assets.open("models/walking_binary.onnx").readBytes()
            binarySession = env.createSession(binaryBytes, OrtSession.SessionOptions())
            Log.d(TAG, "Binary Walking ONNX Model loaded successfully (99.86% Test Acc, Rotation-Invariant).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Binary Walking ONNX model: ${e.message}", e)
        }

        try {
            val threeClassBytes = context.assets.open("models/three_class.onnx").readBytes()
            threeClassSession = env.createSession(threeClassBytes, OrtSession.SessionOptions())
            Log.d(TAG, "Three-Class ONNX Model loaded successfully (87.43% Test Acc).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Three-Class ONNX model: ${e.message}", e)
        }

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
     * @param modelType "walking_binary" (default & recommended), "three_class", "cnn_1d", or "cnn_lstm"
     * @param uncertaintyThreshold Minimum confidence below which result is flagged UNKNOWN.
     * @param enableSmoothing Whether to apply rolling majority voting across consecutive windows.
     */
    fun classifyWindow(
        windowData: FloatArray,
        modelType: String = MODEL_BINARY_WALKING,
        uncertaintyThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
        enableSmoothing: Boolean = true
    ): ClassificationResult {
        val startTime = System.currentTimeMillis()
        val session = when (modelType) {
            MODEL_BINARY_WALKING -> binarySession ?: cnn1dSession
            MODEL_3_CLASS -> threeClassSession ?: cnn1dSession
            MODEL_CNN_LSTM -> cnnLstmSession ?: cnn1dSession
            else -> cnn1dSession ?: binarySession
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

            val effectiveThreshold = when (modelType) {
                MODEL_BINARY_WALKING -> 0.60f
                MODEL_3_CLASS -> 0.50f
                else -> uncertaintyThreshold
            }

            val isUncertain = maxProb < effectiveThreshold
            val rawActivity = if (isUncertain) {
                ActivityType.UNKNOWN
            } else {
                when (modelType) {
                    MODEL_BINARY_WALKING -> ActivityType.fromBinaryIndex(maxIndex)
                    MODEL_3_CLASS -> ActivityType.fromThreeClassIndex(maxIndex)
                    else -> ActivityType.fromIndex(maxIndex)
                }
            }

            // Temporal Smoothing (Majority Voting)
            var smoothedActivity = rawActivity
            if (enableSmoothing && rawActivity != ActivityType.UNKNOWN && rawActivity != ActivityType.UNCERTAIN) {
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
            } else if (rawActivity == ActivityType.UNKNOWN || rawActivity == ActivityType.UNCERTAIN) {
                // When confidence is low, don't keep stale dynamic votes
                predictionHistory.clear()
            }

            val latency = System.currentTimeMillis() - startTime
            inputTensor.close()
            results.close()

            val modelLabel = when (modelType) {
                MODEL_BINARY_WALKING -> "Binary Invariant (99.86% • Handheld)"
                MODEL_3_CLASS -> "3-Class (87.43% • Experimental)"
                MODEL_CNN_LSTM -> "CNN-LSTM (90.74%)"
                else -> "1D CNN (93.99% • UCI HAR)"
            }

            return ClassificationResult(
                activityType = smoothedActivity,
                rawActivityType = rawActivity,
                confidence = maxProb,
                rawConfidence = maxProb,
                logits = rawLogits.clone(),
                probabilities = probabilities.clone(),
                isUncertain = isUncertain,
                inferenceTimeMs = latency,
                modelUsed = modelLabel
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
            binarySession?.close()
            threeClassSession?.close()
            cnn1dSession?.close()
            cnnLstmSession?.close()
            env.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ONNX sessions: ${e.message}", e)
        }
    }

    private fun fallbackResult(modelType: String): ClassificationResult {
        return ClassificationResult(
            activityType = ActivityType.UNKNOWN,
            rawActivityType = ActivityType.UNKNOWN,
            confidence = 0f,
            rawConfidence = 0f,
            logits = FloatArray(2),
            probabilities = FloatArray(2) { 0.5f },
            isUncertain = true,
            inferenceTimeMs = 0L,
            modelUsed = modelType
        )
    }
}
