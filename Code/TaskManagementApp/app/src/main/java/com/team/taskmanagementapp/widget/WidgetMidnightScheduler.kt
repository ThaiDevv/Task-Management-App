package com.team.taskmanagementapp.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Đảm bảo widget đổi nội dung đúng sang "việc của ngày mới" ngay sau nửa đêm,
 * kể cả khi app không hề được mở. Dùng WorkManager periodic 24h với initial delay
 * tính đến 00:00 tiếp theo — không cần AlarmManager exact (tốn pin, cần quyền).
 */
object WidgetMidnightScheduler {

    private const val UNIQUE_WORK_NAME = "task_widget_midnight_refresh"

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val request = PeriodicWorkRequestBuilder<WidgetMidnightWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNextMidnight(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun delayUntilNextMidnight(): Long {
        val now = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return (nextMidnight.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}

/** Worker chỉ làm một việc: bảo các widget vẽ lại từ dữ liệu Room mới nhất. */
class WidgetMidnightWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }
}
