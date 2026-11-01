package com.motionsense.har.sensor

/**
 * 3rd-Order Butterworth Low-Pass Digital Filter (0.3 Hz cutoff at 50 Hz sampling rate).
 * Matches the exact filtering specifications used in the UCI HAR research dataset
 * to separate total acceleration into gravitational component and body motion acceleration.
 *
 * Implemented using Direct Form II Transposed in 64-bit Double precision for zero numerical
 * drift and maximum stability on ARM64 processors.
 */
class ButterworthFilter3rdOrder {

    companion object {
        // Numerator (b) and Denominator (a) coefficients generated via scipy.signal.butter(3, 0.3 / 25.0, btype='low')
        private val B = doubleArrayOf(
            6.451849129090928e-06,
            1.9355547387272784e-05,
            1.9355547387272784e-05,
            6.451849129090928e-06
        )

        private val A = doubleArrayOf(
            1.0,
            -2.9246062354506974,
            2.852027818593894,
            -0.9273699683501638
        )
    }

    private var w1 = 0.0
    private var w2 = 0.0
    private var w3 = 0.0
    private var isInitialized = false

    /**
     * Filters a single sample x and returns low-pass filtered gravity y.
     */
    fun filter(x: Double): Double {
        if (!isInitialized) {
            initDc(x)
            isInitialized = true
        }

        val y = B[0] * x + w1
        w1 = B[1] * x - A[1] * y + w2
        w2 = B[2] * x - A[2] * y + w3
        w3 = B[3] * x - A[3] * y
        return y
    }

    fun filterFloat(x: Float): Float {
        return filter(x.toDouble()).toFloat()
    }

    /**
     * Initializes filter internal states to DC steady-state for the initial input value.
     * Prevents startup transient latency.
     */
    fun initDc(initialValue: Double) {
        w3 = (B[3] - A[3]) * initialValue
        w2 = (B[2] - A[2]) * initialValue + w3
        w1 = (B[1] - A[1]) * initialValue + w2
        isInitialized = true
    }

    fun reset() {
        w1 = 0.0
        w2 = 0.0
        w3 = 0.0
        isInitialized = false
    }
}

/**
 * 3-Axis Butterworth Filter for (X, Y, Z) acceleration streams.
 */
class TriaxialButterworthFilter {
    private val filterX = ButterworthFilter3rdOrder()
    private val filterY = ButterworthFilter3rdOrder()
    private val filterZ = ButterworthFilter3rdOrder()

    /**
     * Filters a 3-axis input (totalAcc) and returns the estimated 3-axis Gravity vector.
     */
    fun filterGravity(totalAcc: FloatArray): FloatArray {
        return floatArrayOf(
            filterX.filterFloat(totalAcc[0]),
            filterY.filterFloat(totalAcc[1]),
            filterZ.filterFloat(totalAcc[2])
        )
    }

    fun reset() {
        filterX.reset()
        filterY.reset()
        filterZ.reset()
    }
}
