package com.team.taskmanagementapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.util.AlarmScheduler
import com.team.taskmanagementapp.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BootReceiver — Khôi phục toàn bộ lịch báo thức (AlarmManager PendingIntents) sau khi
 * thiết bị khởi động lại (reboot). Android xóa sạch tất cả các Alarm khi tắt máy,
 * BootReceiver lắng nghe ACTION_BOOT_COMPLETED và đặt lại từng task còn hiệu lực.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast action: $action")

        val isBootAction = action == Intent.ACTION_BOOT_COMPLETED
                || action == "android.intent.action.QUICKBOOT_POWERON"
                || action == Intent.ACTION_LOCKED_BOOT_COMPLETED

        if (!isBootAction) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                rescheduleAllAlarms(context)
                markOverdueTasks(context)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to restore reminders after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Đặt lại lịch báo thức cho tất cả các task chưa hoàn thành có thời gian nhắc
     * trong tương lai. Task nào đã qua giờ nhắc thì bỏ qua (sẽ xử lý ở markOverdueTasks).
     */
    private suspend fun rescheduleAllAlarms(context: Context) {
        val appContext = context.applicationContext
        val activeTasks = AppDatabase.getInstance(appContext).taskDao().getActiveTasksSync()

        Log.d(TAG, "Rescheduling alarms for ${activeTasks.size} active task(s) after boot.")

        val now = System.currentTimeMillis()
        activeTasks.forEach { task ->
            val triggerAt = AlarmScheduler.calculateTriggerAtMillis(task)
            if (triggerAt != null && triggerAt > now) {
                val res = AlarmScheduler.scheduleAlarm(appContext, task, now)
                Log.d(TAG, "Restored a reminder after boot; result=$res")
            } else {
                // Hủy PendingIntent thừa nếu còn tồn tại
                AlarmScheduler.cancelAlarm(appContext, task.id)
                Log.d(TAG, "Skipped a past reminder after boot")
            }
        }
    }

    /**
     * Kiểm tra các task chưa hoàn thành có thời gian đến hạn đã qua trong lúc máy
     * tắt → cập nhật trạng thái sang OVERDUE để UI phản ánh chính xác khi mở app.
     */
    private suspend fun markOverdueTasks(context: Context) {
        val appContext = context.applicationContext
        val dao = AppDatabase.getInstance(appContext).taskDao()
        val now = System.currentTimeMillis()
        val activeTasks = dao.getActiveTasksSync()

        activeTasks.forEach { task ->
            if (task.isCompleted || task.status == TaskStatus.COMPLETED) return@forEach

            val combinedDue = DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
            val isOverdue = combinedDue > 0L && combinedDue < now

            if (isOverdue && task.status != TaskStatus.OVERDUE) {
                Log.d(TAG, "Marking an overdue task after boot")
                dao.updateTask(task.copy(status = TaskStatus.OVERDUE, updatedAt = now))
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
