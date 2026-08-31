package com.team.taskmanagementapp.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.receiver.TaskNotificationReceiver
import com.team.taskmanagementapp.ui.detail.TaskDetailActivity

/** Utility for creating, showing, and cancelling task reminder notifications. */
object NotificationHelper {

    /** Returns false when app notifications, runtime access, or the reminder channel is blocked. */
    fun areNotificationsEnabled(context: Context): Boolean {
        val applicationContext = context.applicationContext
        val enabledByUser = applicationContext.getSharedPreferences(
            Constants.PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, true)
        if (!enabledByUser) return false

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val enabledBySystem = runCatching {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        }.onFailure {
            Log.w(TAG, "Unable to query app notification state", it)
        }.getOrDefault(false)
        if (!enabledBySystem) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelEnabled = runCatching {
                val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
                val channel = manager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID)
                channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
            }.onFailure {
                Log.w(TAG, "Unable to query reminder channel state", it)
            }.getOrDefault(false)
            if (!channelEnabled) return false
        }

        return true
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val existingChannel = notificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID)
            // Never delete/recreate an existing channel: that could disregard the user's choice.
            if (existingChannel != null) return

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Constants.NOTIFICATION_CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(soundUri, audioAttributes)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminder(context: Context, task: Task) {
        if (!areNotificationsEnabled(context)) {
            Log.w(TAG, "Reminder notification not shown because notifications are disabled")
            return
        }

        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Opens the task detail screen when the notification body is tapped.
        val detailIntent = Intent(context, TaskDetailActivity::class.java).apply {
            putExtra(Constants.EXTRA_TASK_ID, task.id.toLong())
        }
        val detailPendingIntent = PendingIntent.getActivity(
            context,
            requestCode(task.id, REQUEST_OPEN_DETAIL),
            detailIntent,
            pendingIntentFlags
        )

        val completeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_MARK_COMPLETE
            putExtra(Constants.EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(task.id, REQUEST_MARK_COMPLETE),
            completeIntent,
            pendingIntentFlags
        )

        val snoozeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_SNOOZE_15_MINUTES
            putExtra(Constants.EXTRA_TASK_ID, task.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(task.id, REQUEST_SNOOZE),
            snoozeIntent,
            pendingIntentFlags
        )

        createNotificationChannel(context)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(
            context,
            Constants.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification_task)
            .setContentTitle(task.title)
            .setContentText(task.description.ifBlank { "Task Reminder" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(task.description.ifBlank { "Task Reminder" }))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(detailPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.notification_action_complete),
                completePendingIntent
            )
            .addAction(
                R.drawable.ic_time,
                context.getString(R.string.notification_action_snooze),
                snoozePendingIntent
            )
            .build()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Reminder notification not shown because runtime access is missing")
            return
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(task.id, notification)
        }.onFailure {
            Log.w(TAG, "Unable to post reminder notification", it)
        }
    }

    fun cancelNotification(context: Context, taskId: Int) {
        NotificationManagerCompat.from(context).cancel(taskId)
    }

    private fun mapPriority(priority: Priority): Int {
        return when (priority) {
            Priority.LOW -> NotificationCompat.PRIORITY_LOW
            Priority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
            Priority.HIGH,
            Priority.URGENT -> NotificationCompat.PRIORITY_HIGH
        }
    }

    private fun requestCode(taskId: Int, actionCode: Int): Int = taskId * 10 + actionCode

    private const val REQUEST_OPEN_DETAIL = 0
    private const val REQUEST_MARK_COMPLETE = 1
    private const val REQUEST_SNOOZE = 2
    private const val TAG = "NotificationHelper"
}
