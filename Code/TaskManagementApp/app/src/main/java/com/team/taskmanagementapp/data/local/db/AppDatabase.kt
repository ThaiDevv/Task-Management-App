package com.team.taskmanagementapp.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.team.taskmanagementapp.data.local.dao.PomodoroDao
import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.entity.PomodoroSession
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.local.db.Converters
import com.team.taskmanagementapp.util.Constants

@Database(
    entities = [Task::class, PomodoroSession::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun pomodoroDao(): PomodoroDao

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

        /**
         * v2 → v3: công việc lặp lại nâng cao (schema này do `main` định nghĩa).
         *
         * 1. `repeatEndDate`: mốc kết thúc chuỗi lặp.
         * 2. `repeatLimitCount`: giới hạn số lần lặp.
         * 3. `currentOccurrence`: lần lặp hiện tại (bắt đầu từ 1).
         * 4. `isPaused`: tạm dừng chuỗi lặp.
         *
         * Tất cả đều có DEFAULT nên dữ liệu Task hiện có được giữ nguyên.
         * Để `internal` cho test migration (androidTest) truy cập được.
         */
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("tasks", "repeatEndDate", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("tasks", "repeatLimitCount", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("tasks", "currentOccurrence", "INTEGER NOT NULL DEFAULT 1")
                db.addColumnIfMissing("tasks", "isPaused", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v3 → v4: bổ sung tính năng Pomodoro Timer.
         *
         * 1. Tạo bảng `pomodoro_sessions` (lịch sử các phiên tập trung/nghỉ).
         * 2. Khóa ngoại `taskId` → `tasks.id` với ON DELETE CASCADE
         *    (xoá Task thì xoá luôn lịch sử Pomodoro của Task đó).
         * 3. Index cho `taskId` (tăng tốc JOIN/query theo task) và `startTime`
         *    (tăng tốc các query thống kê theo ngày/tuần).
         * 4. Thêm 3 cột theo dõi Pomodoro vào bảng `tasks` với DEFAULT 0
         *    → toàn bộ dữ liệu Task hiện có được giữ nguyên, không mất mát.
         *
         * DDL dưới đây khớp CHÍNH XÁC với schema mà Room sinh ra cho
         * [PomodoroSession] và [Task] ở version 4, nếu không khớp Room sẽ báo
         * "Migration didn't properly handle ..." khi mở database.
         *
         * Lưu ý khi merge: nhánh Pomodoro từng dùng chính số 3 cho schema Pomodoro, nên
         * migration ở đây thêm cột theo kiểu "nếu chưa có" và bổ sung luôn 4 cột lặp-lịch
         * để DB ở biến thể v3 nào cũng về đúng schema v4.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1 + 2. Bảng pomodoro_sessions kèm khóa ngoại tới tasks
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pomodoro_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER NOT NULL, " +
                        "`startTime` INTEGER NOT NULL, " +
                        "`endTime` INTEGER NOT NULL, " +
                        "`durationInMinutes` INTEGER NOT NULL, " +
                        "`sessionType` TEXT NOT NULL, " +
                        "`isCompleted` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )

                // 3. Index
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pomodoro_sessions_taskId` " +
                        "ON `pomodoro_sessions` (`taskId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pomodoro_sessions_startTime` " +
                        "ON `pomodoro_sessions` (`startTime`)"
                )

                // 4. Cột theo dõi Pomodoro trên bảng tasks (giữ nguyên dữ liệu cũ)
                db.addColumnIfMissing("tasks", "estimatedPomodoros", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("tasks", "completedPomodoros", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("tasks", "totalFocusTimeMinutes", "INTEGER NOT NULL DEFAULT 0")

                // 4b. Cột `completedAt` (từ `main`): mốc hoàn thành thật của task.
                //     Nullable và không DEFAULT ⇒ task cũ giữ nguyên "chưa rõ ngày hoàn thành".
                db.addColumnIfMissing("tasks", "completedAt", "INTEGER")

                // 5. Cột lặp-lịch: cần cho DB đang ở "v3 của nhánh Pomodoro" (không có các cột này).
                //    Nếu DB đến từ v3 của main thì các cột đã tồn tại nên bước này không làm gì.
                db.addColumnIfMissing("tasks", "repeatEndDate", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("tasks", "repeatLimitCount", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("tasks", "currentOccurrence", "INTEGER NOT NULL DEFAULT 1")
                db.addColumnIfMissing("tasks", "isPaused", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Thêm cột nếu chưa tồn tại.
         *
         * SQLite không có `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, nên phải hỏi
         * `PRAGMA table_info`. Nhờ vậy các migration ở trên luôn chạy lại được mà không
         * lỗi "duplicate column name" (quan trọng khi hai nhánh cùng đánh số version 3).
         */
        private fun SupportSQLiteDatabase.addColumnIfMissing(
            table: String,
            column: String,
            definition: String
        ) {
            val alreadyExists = query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                var found = false
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == column) {
                        found = true
                        break
                    }
                }
                found
            }

            if (!alreadyExists) {
                execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
            }
        }
    }
}
