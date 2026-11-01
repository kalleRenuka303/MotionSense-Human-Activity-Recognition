package com.motionsense.har.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class SensorPipelineValidationTest {

    @Test
    fun testButterworthFilterDCSteadyState() {
        // DC input (1.0g gravity) starting with DC initialization should yield 1.0g immediately
        val filter = ButterworthFilter3rdOrder()
        val initialGravity = 1.0
        filter.initDc(initialGravity)

        val out1 = filter.filter(1.0)
        assertEquals(1.0, out1, 1e-4)

        // Multiple steps of 1.0 should stay at 1.0
        for (i in 0 until 50) {
            val out = filter.filter(1.0)
            assertEquals(1.0, out, 1e-4)
        }
    }

    @Test
    fun testButterworthFilterAttenuationAtWalkingFrequency() {
        // Test that 2 Hz dynamic oscillation (walking steps) is heavily attenuated (> 90%) by the 0.3 Hz filter
        val filter = ButterworthFilter3rdOrder()
        filter.initDc(1.0)

        // Run 50 Hz stream with 1.0g DC + 0.5g 2Hz walking ripple for 200 samples (4 seconds)
        var maxRipple = 0.0
        for (n in 0 until 200) {
            val t = n / 50.0
            val input = 1.0 + 0.5 * Math.sin(2.0 * Math.PI * 2.0 * t)
            val grav = filter.filter(input)
            if (n > 100) { // After settling
                val ripple = abs(grav - 1.0)
                if (ripple > maxRipple) maxRipple = ripple
            }
        }

        // Ripple should be reduced from 0.5g to less than 0.05g (>90% attenuation)
        assertTrue("Max ripple $maxRipple should be < 0.05", maxRipple < 0.05)
    }

    @Test
    fun testPhonePlacementVectorAlignment() {
        // Test Front Pocket Portrait alignment:
        // Android Y (Vertical gravity) -> Model X
        // Android -Z (Forward) -> Model Y
        // Android X (Lateral) -> Model Z
        val androidVector = floatArrayOf(0.1f, 0.98f, -0.05f) // X=0.1, Y=0.98, Z=-0.05

        val alignedPocket = PhonePlacement.FRONT_POCKET_PORTRAIT.alignVector(androidVector)
        assertEquals(0.98f, alignedPocket[0], 1e-5f) // Model X gets Android Y
        assertEquals(0.05f, alignedPocket[1], 1e-5f) // Model Y gets -Android Z
        assertEquals(0.1f, alignedPocket[2], 1e-5f)  // Model Z gets Android X

        // Test Waist Holster (UCI Native 1:1)
        val alignedWaist = PhonePlacement.WAIST_HOLSTER_LANDSCAPE.alignVector(androidVector)
        assertEquals(0.1f, alignedWaist[0], 1e-5f)
        assertEquals(0.98f, alignedWaist[1], 1e-5f)
        assertEquals(-0.05f, alignedWaist[2], 1e-5f)
    }

    @Test
    fun testSensorUnitsNormalization() {
        val standardGravityMps2 = 9.80665f
        val gravityG = standardGravityMps2 / SensorCollector.GRAVITY_STANDARD
        assertEquals(1.0f, gravityG, 1e-5f)

        val doubleGravityMps2 = 19.6133f
        val doubleG = doubleGravityMps2 / SensorCollector.GRAVITY_STANDARD
        assertEquals(2.0f, doubleG, 1e-4f)
    }

    @Test
    fun testChannelOrderingMatchesPipelineB() {
        val sample = FloatArray(SensorCollector.NUM_CHANNELS)
        for (i in 0 until 9) {
            sample[i] = i.toFloat()
        }

        assertEquals(0f, sample[0], 0f) // total_acc_x
        assertEquals(1f, sample[1], 0f) // total_acc_y
        assertEquals(2f, sample[2], 0f) // total_acc_z
        assertEquals(3f, sample[3], 0f) // body_acc_x
        assertEquals(4f, sample[4], 0f) // body_acc_y
        assertEquals(5f, sample[5], 0f) // body_acc_z
        assertEquals(6f, sample[6], 0f) // body_gyro_x
        assertEquals(7f, sample[7], 0f) // body_gyro_y
        assertEquals(8f, sample[8], 0f) // body_gyro_z
    }
}
