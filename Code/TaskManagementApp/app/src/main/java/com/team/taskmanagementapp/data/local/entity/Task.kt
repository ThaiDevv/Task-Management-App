package com.team.taskmanagementapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.team.taskmanagementapp.data.model.enum.Priority
import com.team.taskmanagementapp.data.model.enum.RecurrenceType
import com.team.taskmanagementapp.data.model.enum.TaskStatus

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
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
)