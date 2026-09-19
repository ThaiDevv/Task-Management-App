package com.team.taskmanagementapp.util

import android.content.Context
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus

/**
 * Helper dùng chung cho các điểm hoàn thành task NGOÀI UI chính
 * (widget màn hình chính, action trên notification).
 *
 * Logic mirror theo [com.team.taskmanagementapp.viewmodel.TaskViewModel.toggleTaskComplete]:
 * cập nhật trạng thái, hủy/đặt lại alarm + notification, và xử lý recurrence
 * (tạo instance kế tiếp khi hoàn thành, dọn instance tự sinh khi hủy hoàn thành).
 */
object TaskCompletionHelper {

    /**
     * Đảo trạng thái hoàn thành của task theo id.
     * @return true nếu có thay đổi thực sự, false nếu task không tồn tại hoặc không đổi trạng thái.
     */
    suspend fun toggleById(context: Context, taskId: Int): Boolean {
        val appContext = context.applicationContext
        val task = AppDatabase.getInstance(appContext)
            .taskDao()
            .getTaskById(taskId.toLong())
        android.util.Log.d("TaskCompletionHelper", "toggleById id=$taskId task=${task?.title}")
        if (task == null) return false
        return setCompleted(appContext, task, targetCompleted = !task.isCompleted)
    }

    /**
     * Đặt trạng thái hoàn thành cho task, kèm đầy đủ tác dụng phụ (alarm, notification, recurrence).
     */
    suspend fun setCompleted(context: Context, task: Task, targetCompleted: Boolean): Boolean {
        if (task.isCompleted == targetCompleted &&
            (targetCompleted || task.status != TaskStatus.COMPLETED)
        ) return false

        val appContext = context.applicationContext
        val dao = AppDatabase.getInstance(appContext).taskDao()
        val now = System.currentTimeMillis()

        val updatedTask = task.copy(
            isCompleted = targetCompleted,
            status = if (targetCompleted) TaskStatus.COMPLETED else TaskStatus.TODO,
            updatedAt = now
        )
        dao.updateTask(updatedTask)

        if (targetCompleted) {
            AlarmScheduler.cancelAlarm(appContext, updatedTask.id)
            NotificationHelper.cancelNotification(appContext, updatedTask.id)
        } else {
            AlarmScheduler.scheduleAlarm(appContext, updatedTask)
        }

        val isRecurringTask = task.isRecurring || task.recurrenceType != RecurrenceType.NONE
        if (isRecurringTask) {
            handleRecurrenceSideEffects(appContext, dao, task, targetCompleted, now)
        }
        return true
    }

    /**
     * Khi hoàn thành một task lặp lại: tạo instance kế tiếp nếu chưa có task tương lai cùng title.
     * Khi hủy hoàn thành: xóa các instance tương lai chưa hoàn thành đã tự sinh ra trước đó.
     */
    private suspend fun handleRecurrenceSideEffects(
        appContext: Context,
        dao: com.team.taskmanagementapp.data.local.dao.TaskDao,
        originalTask: Task,
        targetCompleted: Boolean,
        now: Long
    ) {
        val nextDueDate = RecurrenceHelper.calculateNextDueDate(
            originalTask.dueDate, originalTask.recurrenceType, originalTask.recurrenceInterval
        )
        val nextDueTime = if (originalTask.dueTime > 0L) {
            RecurrenceHelper.calculateNextDueDate(
                originalTask.dueTime, originalTask.recurrenceType, originalTask.recurrenceInterval
            )
        } else originalTask.dueTime

        if (targetCompleted) {
            val isEnded = RecurrenceHelper.isRecurrenceEnded(originalTask, nextDueDate)
            if (!isEnded && !originalTask.isPaused) {
                val existingFutureTasks = dao.getFutureRecurringTasksSync(
                    originalTask.title, originalTask.recurrenceType, nextDueDate
                )
                val anyRecurringFutureTasks = if (existingFutureTasks.isNotEmpty()) {
                    existingFutureTasks
                } else {
                    dao.getFutureRecurringTasksSync(originalTask.title, RecurrenceType.DAILY, nextDueDate) +
                        dao.getFutureRecurringTasksSync(originalTask.title, RecurrenceType.WEEKLY, nextDueDate) +
                        dao.getFutureRecurringTasksSync(originalTask.title, RecurrenceType.MONTHLY, nextDueDate) +
                        dao.getFutureRecurringTasksSync(originalTask.title, RecurrenceType.YEARLY, nextDueDate)
                }

                if (anyRecurringFutureTasks.isEmpty()) {
                    val nextInstance = originalTask.copy(
                        id = 0,
                        isCompleted = false,
                        status = TaskStatus.TODO,
                        isRecurring = true,
                        recurrenceType = originalTask.recurrenceType,
                        dueDate = nextDueDate,
                        dueTime = nextDueTime,
                        repeatEndDate = originalTask.repeatEndDate,
                        repeatLimitCount = originalTask.repeatLimitCount,
                        currentOccurrence = originalTask.currentOccurrence + 1,
                        isPaused = originalTask.isPaused,
                        createdAt = now,
                        updatedAt = now
                    )
                    val insertedId = dao.insertTask(nextInstance)
                    AlarmScheduler.scheduleAlarm(
                        appContext, nextInstance.copy(id = insertedId.toInt())
                    )
                }
            }
        } else {
            val futureTasks = (
                dao.getFutureRecurringTasksSync(originalTask.title, originalTask.recurrenceType, nextDueDate) +
                    dao.getFutureRecurringTasksSync(originalTask.title, RecurrenceType.DAILY, nextDueDate) +
                    dao.getFutureRecurringTasksSync(originalTask.title, RecurrenceType.WEEKLY, nextDueDate) +
                    dao.getFutureRecurringTasksSync(originalTask.title, RecurrenceType.MONTHLY, nextDueDate) +
                    dao.getFutureRecurringTasksSync(originalTask.title, RecurrenceType.YEARLY, nextDueDate)
                ).distinctBy { it.id }

            futureTasks.forEach { futureTask ->
                AlarmScheduler.cancelAlarm(appContext, futureTask.id)
                dao.deleteTask(futureTask)
            }
        }
    }
}
