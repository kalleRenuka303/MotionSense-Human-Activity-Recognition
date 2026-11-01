package com.motionsense.har.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTypeTest {

    @Test
    fun testAllActivitiesExist() {
        val types = ActivityType.values()
        assertTrue(types.any { it.name == "WALKING" })
        assertTrue(types.any { it.name == "WALKING_UPSTAIRS" })
        assertTrue(types.any { it.name == "WALKING_DOWNSTAIRS" })
        assertTrue(types.any { it.name == "SITTING" })
        assertTrue(types.any { it.name == "STANDING" })
        assertTrue(types.any { it.name == "LAYING" })
        assertTrue(types.any { it.name == "UNCERTAIN" })
    }

    @Test
    fun testActivityDisplayNamesAndCategories() {
        assertEquals("Walking", ActivityType.WALKING.displayName)
        assertEquals("Dynamic", ActivityType.WALKING.category)

        assertEquals("Walking Upstairs", ActivityType.WALKING_UPSTAIRS.displayName)
        assertEquals("Dynamic", ActivityType.WALKING_UPSTAIRS.category)

        assertEquals("Walking Downstairs", ActivityType.WALKING_DOWNSTAIRS.displayName)
        assertEquals("Dynamic", ActivityType.WALKING_DOWNSTAIRS.category)

        assertEquals("Sitting", ActivityType.SITTING.displayName)
        assertEquals("Static", ActivityType.SITTING.category)

        assertEquals("Standing", ActivityType.STANDING.displayName)
        assertEquals("Static", ActivityType.STANDING.category)

        assertEquals("Laying", ActivityType.LAYING.displayName)
        assertEquals("Static", ActivityType.LAYING.category)
    }

    @Test
    fun testFromIndex() {
        assertEquals(ActivityType.WALKING, ActivityType.fromIndex(0))
        assertEquals(ActivityType.WALKING_UPSTAIRS, ActivityType.fromIndex(1))
        assertEquals(ActivityType.WALKING_DOWNSTAIRS, ActivityType.fromIndex(2))
        assertEquals(ActivityType.SITTING, ActivityType.fromIndex(3))
        assertEquals(ActivityType.STANDING, ActivityType.fromIndex(4))
        assertEquals(ActivityType.LAYING, ActivityType.fromIndex(5))
        assertEquals(ActivityType.UNCERTAIN, ActivityType.fromIndex(99))
    }
}
