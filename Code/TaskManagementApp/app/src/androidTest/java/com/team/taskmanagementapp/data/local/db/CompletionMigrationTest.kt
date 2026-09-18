package com.team.taskmanagementapp.data.local.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CompletionMigrationTest {
    @Test fun upgradeFromVersion2PreservesLegacyTasksWithoutInventingDates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "completion-migration-test-${System.nanoTime()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
                db.execSQL("""CREATE TABLE tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL, description TEXT NOT NULL,
                    dueDate INTEGER NOT NULL, dueTime INTEGER NOT NULL,
                    priority TEXT NOT NULL, status TEXT NOT NULL,
                    isComplete INTEGER NOT NULL, isRecurring INTEGER NOT NULL,
                    recurrenceType TEXT NOT NULL, recurrenceInterval INTEGER NOT NULL,
                    reminderMinutes INTEGER NOT NULL, createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL)""")
                db.execSQL("""INSERT INTO tasks VALUES
                    (1, 'Legacy task', 'Keep me', 100, 200, 'MEDIUM', 'COMPLETED',
                     1, 0, 'NONE', 1, 15, 300, 400)""")
                db.version = 2
            }
            val room = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_2_3).build()
            try {
                val tasks = room.taskDao().getAllTasksSync()
                assertEquals(1, tasks.size)
                assertEquals("Legacy task", tasks.single().title)
                assertEquals("Keep me", tasks.single().description)
                assertTrue(tasks.single().isCompleted)
                assertEquals(15, tasks.single().reminderMinutes)
                assertEquals(400L, tasks.single().updatedAt)
                assertNull(tasks.single().completedAt)
            } finally { room.close() }
        } finally { context.deleteDatabase(name) }
    }
}
