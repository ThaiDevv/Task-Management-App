package com.team.taskmanagementapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.team.taskmanagementapp.data.model.enums.SessionType

/**
 * Lịch sử một phiên Pomodoro (tập trung hoặc nghỉ).
 *
 * Liên kết tới [Task] qua [taskId]: khi Task bị xoá, toàn bộ phiên Pomodoro
 * của Task đó cũng bị xoá theo (CASCADE) để tránh dữ liệu mồ côi.
 *
 * Đơn vị thời gian: mốc thời gian tuyệt đối tính bằng milliseconds
 * (System.currentTimeMillis()), đồng nhất với `dueDate`/`createdAt` của [Task].
 */
@Entity(
    tableName = "pomodoro_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["startTime"])
    ]
)
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "taskId")
    val taskId: Long,
    @ColumnInfo(name = "startTime")
    val startTime: Long,
    @ColumnInfo(name = "endTime")
    val endTime: Long,
    @ColumnInfo(name = "durationInMinutes")
    val durationInMinutes: Int,
    @ColumnInfo(name = "sessionType")
    val sessionType: SessionType = SessionType.FOCUS,
    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false
)
