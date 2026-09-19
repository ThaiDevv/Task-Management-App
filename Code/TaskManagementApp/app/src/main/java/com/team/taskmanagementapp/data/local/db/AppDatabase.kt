package com.team.taskmanagementapp.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.local.db.Converters
import com.team.taskmanagementapp.util.Constants

@Database(
    entities = [Task::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN reminderMinutes INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN repeatEndDate INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN repeatLimitCount INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN currentOccurrence INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val existingColumns = mutableSetOf<String>()
                db.query("PRAGMA table_info(tasks)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        existingColumns += cursor.getString(nameIndex)
                    }
                }

                // Main version 3 already has the recurrence columns. Older PR builds used
                // version 3 for completedAt, so fill whichever columns are still missing.
                if ("repeatEndDate" !in existingColumns) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN repeatEndDate INTEGER NOT NULL DEFAULT 0")
                }
                if ("repeatLimitCount" !in existingColumns) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN repeatLimitCount INTEGER NOT NULL DEFAULT 0")
                }
                if ("currentOccurrence" !in existingColumns) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN currentOccurrence INTEGER NOT NULL DEFAULT 1")
                }
                if ("isPaused" !in existingColumns) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0")
                }
                if ("completedAt" !in existingColumns) {
                    // Preserve existing task data; legacy completion dates remain unknown.
                    db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER")
                }
            }
        }
    }
}
