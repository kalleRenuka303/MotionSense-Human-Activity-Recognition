package com.motionsense.har.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.motionsense.har.data.model.ActivityRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MotionSenseDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val _recordsFlow = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val recordsFlow: Flow<List<ActivityRecord>> = _recordsFlow.asStateFlow()

    companion object {
        const val DATABASE_NAME = "motionsense_har.db"
        const val DATABASE_VERSION = 1

        const val TABLE_RECORDS = "activity_records"
        const val COL_ID = "id"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_ACTIVITY_NAME = "activity_name"
        const val COL_CONFIDENCE = "confidence"
        const val COL_DURATION_SEC = "duration_seconds"
        const val COL_MODEL_USED = "model_used"
        const val COL_DATE = "date_string"

        @Volatile
        private var INSTANCE: MotionSenseDatabase? = null

        fun getInstance(context: Context): MotionSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MotionSenseDatabase(context.applicationContext).also {
                    INSTANCE = it
                    it.refreshCache()
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSql = """
            CREATE TABLE $TABLE_RECORDS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_ACTIVITY_NAME TEXT NOT NULL,
                $COL_CONFIDENCE REAL NOT NULL,
                $COL_DURATION_SEC INTEGER NOT NULL,
                $COL_MODEL_USED TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createSql)
        db.execSQL("CREATE INDEX idx_timestamp ON $TABLE_RECORDS($COL_TIMESTAMP DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        onCreate(db)
    }

    suspend fun insertRecord(record: ActivityRecord): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, record.timestamp)
            put(COL_ACTIVITY_NAME, record.activityName)
            put(COL_CONFIDENCE, record.confidence)
            put(COL_DURATION_SEC, record.durationSeconds)
            put(COL_MODEL_USED, record.modelUsed)
            put(COL_DATE, record.dateString)
        }
        val id = db.insert(TABLE_RECORDS, null, values)
        refreshCache()
        id
    }

    suspend fun getAllRecords(): List<ActivityRecord> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ActivityRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RECORDS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ActivityRecord(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        activityName = it.getString(it.getColumnIndexOrThrow(COL_ACTIVITY_NAME)),
                        confidence = it.getFloat(it.getColumnIndexOrThrow(COL_CONFIDENCE)),
                        durationSeconds = it.getLong(it.getColumnIndexOrThrow(COL_DURATION_SEC)),
                        modelUsed = it.getString(it.getColumnIndexOrThrow(COL_MODEL_USED)),
                        dateString = it.getString(it.getColumnIndexOrThrow(COL_DATE))
                    )
                )
            }
        }
        list
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_RECORDS, null, null)
        refreshCache()
    }

    fun refreshCache() {
        try {
            val db = readableDatabase
            val list = mutableListOf<ActivityRecord>()
            val cursor = db.query(TABLE_RECORDS, null, null, null, null, null, "$COL_TIMESTAMP DESC")
            cursor.use {
                while (it.moveToNext()) {
                    list.add(
                        ActivityRecord(
                            id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                            timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                            activityName = it.getString(it.getColumnIndexOrThrow(COL_ACTIVITY_NAME)),
                            confidence = it.getFloat(it.getColumnIndexOrThrow(COL_CONFIDENCE)),
                            durationSeconds = it.getLong(it.getColumnIndexOrThrow(COL_DURATION_SEC)),
                            modelUsed = it.getString(it.getColumnIndexOrThrow(COL_MODEL_USED)),
                            dateString = it.getString(it.getColumnIndexOrThrow(COL_DATE))
                        )
                    )
                }
            }
            _recordsFlow.value = list
        } catch (e: Exception) {
            // Database not ready yet or closing
        }
    }
}
