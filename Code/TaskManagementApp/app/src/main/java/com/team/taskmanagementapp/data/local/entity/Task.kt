package com.team.taskmanagementapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus

@Entity(tableName = "tasks")
data class Task (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "dueDate")
    val dueDate: Long,
    @ColumnInfo(name = "dueTime")
    val dueTime: Long,
    @ColumnInfo(name = "priority")
    val priority: Priority,
    @ColumnInfo(name = "status")
    val status: TaskStatus = TaskStatus.TODO,
    @com.google.gson.annotations.SerializedName("isCompleted", alternate = ["isComplete"])
    @ColumnInfo(name = "isComplete")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "isRecurring")
    val isRecurring: Boolean = false,
    @ColumnInfo(name = "recurrenceType")
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    @ColumnInfo(name = "recurrenceInterval")
    val recurrenceInterval: Int = 1,
    @ColumnInfo(name = "reminderMinutes")
    val reminderMinutes: Int = 0,
    @ColumnInfo(name = "repeatEndDate")
    val repeatEndDate: Long = 0L,
    @ColumnInfo(name = "repeatLimitCount")
    val repeatLimitCount: Int = 0,
    @ColumnInfo(name = "currentOccurrence")
    val currentOccurrence: Int = 1,
    @ColumnInfo(name = "isPaused")
    val isPaused: Boolean = false,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    // ── Pomodoro Timer tracking ──────────────────────────────────────────────
    /** Số phiên tập trung (FOCUS) người dùng dự kiến dành cho task này. */
    @ColumnInfo(name = "estimatedPomodoros", defaultValue = "0")
    val estimatedPomodoros: Int = 0,
    /** Số phiên tập trung (FOCUS) đã hoàn thành cho task này. */
    @ColumnInfo(name = "completedPomodoros", defaultValue = "0")
    val completedPomodoros: Int = 0,
    /** Tổng số phút đã tập trung cho task này (tích luỹ từ các phiên FOCUS). */
    @ColumnInfo(name = "totalFocusTimeMinutes", defaultValue = "0")
    val totalFocusTimeMinutes: Int = 0,
)