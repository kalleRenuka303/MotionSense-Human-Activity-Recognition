package com.motionsense.har.sensor

/**
 * Phone Placement and Orientation Alignment Modes.
 *
 * In the original UCI HAR dataset, the smartphone was mounted horizontally on the waist
 * (landscape orientation), meaning Earth's gravity for all upright activities (walking, stairs,
 * sitting, standing) was aligned with the smartphone's X-axis (~1.0g).
 *
 * In physical smartphones, users commonly carry phones vertically in a front trouser pocket
 * or hold them in portrait orientation, where gravity is along the Y-axis.
 *
 * This alignment layer maps physical smartphone sensor axes into the UCI HAR reference frame
 * so that machine learning inference receives signals aligned with the training distribution.
 */
enum class PhonePlacement(
    val title: String,
    val description: String
) {
    FRONT_POCKET_PORTRAIT(
        title = "Front Pocket (Portrait)",
        description = "Phone carried vertically in front trouser pocket with top facing UP. Aligns Android +Y to Model X (gravity)."
    ),
    WAIST_HOLSTER_LANDSCAPE(
        title = "Waist Holster (UCI Native)",
        description = "Phone mounted horizontally on waist belt. Identical to the original UCI HAR experimental setup."
    ),
    HANDHELD_PORTRAIT(
        title = "Handheld (Portrait)",
        description = "Phone held vertically in front of user while walking. Tilted ~30° forward from vertical."
    );

    /**
     * Maps physical Android 3-axis vectors (accel, body accel, or gyro) into the UCI HAR model reference frame.
     */
    fun alignVector(input: FloatArray): FloatArray {
        val x = input[0]
        val y = input[1]
        val z = input[2]

        return when (this) {
            FRONT_POCKET_PORTRAIT -> {
                // Vertical (Y_phone) -> Model X (Gravity axis in UCI HAR)
                // Forward (-Z_phone) -> Model Y (Forward walking direction in UCI HAR)
                // Lateral (X_phone) -> Model Z (Lateral sway in UCI HAR)
                floatArrayOf(
                    y,
                    -z,
                    x
                )
            }
            WAIST_HOLSTER_LANDSCAPE -> {
                // 1:1 mapping directly matching UCI HAR waist landscape holster
                floatArrayOf(x, y, z)
            }
            HANDHELD_PORTRAIT -> {
                // Handheld with ~30° forward tilt
                // cos(30°) ≈ 0.866, sin(30°) ≈ 0.500
                val vert = 0.866f * y - 0.500f * z
                val fwd = -0.866f * z - 0.500f * y
                floatArrayOf(
                    vert,
                    fwd,
                    x
                )
            }
        }
    }
}
