package com.team.taskmanagementapp.data.local.db

import androidx.room.TypeConverter
import com.team.taskmanagementapp.data.model.enum.Priority
import com.team.taskmanagementapp.data.model.enum.RecurrenceType
import com.team.taskmanagementapp.data.model.enum.TaskStatus

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String{
        return priority.name;
    }
    @TypeConverter
    fun toPriority(value: String): Priority {
        return Priority.valueOf(value);
    }
    @TypeConverter
    fun fromRecurrenceType(recurrenceType: RecurrenceType): String{
        return recurrenceType.name
    }
    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType {
        return RecurrenceType.valueOf(value)
    }
    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }
    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus {
        return TaskStatus.valueOf(value)
    }
}