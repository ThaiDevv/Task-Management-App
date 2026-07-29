package com.team.taskmanagementapp.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

    // Khai báo DAO để các tầng khác sử dụng
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Lấy Database Instance duy nhất trong toàn bộ App (Singleton Pattern).
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
