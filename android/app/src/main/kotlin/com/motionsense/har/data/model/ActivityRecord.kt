package com.motionsense.har.data.model

data class ActivityRecord(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val activityName: String,
    val confidence: Float,
    val durationSeconds: Long,
    val modelUsed: String,
    val dateString: String
)

enum class ActivityType(val displayName: String, val category: String) {
    WALKING("Walking", "Dynamic"),
    WALKING_UPSTAIRS("Walking Upstairs", "Dynamic"),
    WALKING_DOWNSTAIRS("Walking Downstairs", "Dynamic"),
    SITTING("Sitting", "Static"),
    STANDING("Standing", "Static"),
    LAYING("Laying", "Static"),
    UNCERTAIN("Uncertain Motion", "Unknown");

    companion object {
        fun fromIndex(index: Int): ActivityType = when (index) {
            0 -> WALKING
            1 -> WALKING_UPSTAIRS
            2 -> WALKING_DOWNSTAIRS
            3 -> SITTING
            4 -> STANDING
            5 -> LAYING
            else -> UNCERTAIN
        }
    }
}
