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
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    // Null for unfinished tasks and legacy backups with no known completion date.
    @ColumnInfo(name = "completedAt")
    val completedAt: Long? = null,
)

/** Keep the completion event independent of later content edits. */
fun Task.withCompletionTracking(previous: Task?, now: Long): Task {
    val done = isCompleted || status == TaskStatus.COMPLETED
    val wasDone = previous?.let { it.isCompleted || it.status == TaskStatus.COMPLETED } == true
    return copy(
        isCompleted = done,
        status = if (done) TaskStatus.COMPLETED else status,
        completedAt = when {
            !done -> null
            wasDone -> previous?.completedAt // Unknown legacy dates remain unknown.
            else -> now
        }
    )
}
