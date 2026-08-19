package com.team.taskmanagementapp.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team.taskmanagementapp.R

/**
 * TASK-39: Handle Notification Permission (Android 13+)
 *
 * Centralized helper for runtime notification permission management.
 * On Android 13+ (API 33), POST_NOTIFICATIONS is a runtime permission that
 * must be explicitly requested and granted by the user.
 */
object NotificationPermissionManager {

    /**
     * Returns true if the app can post notifications.
     * - On API < 33: always true (no runtime permission needed).
     * - On API 33+: checks POST_NOTIFICATIONS runtime permission.
     */
    fun isGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-Android 13: permission is auto-granted
        }
    }

    /**
     * Launches the system permission request dialog on Android 13+.
     */
    fun request(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Shows a rationale dialog explaining WHY the app needs notification permission.
     */
    fun showRationaleDialog(
        activity: Activity,
        launcher: ActivityResultLauncher<String>
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.notif_permission_rationale_title))
            .setMessage(activity.getString(R.string.notif_permission_rationale_message))
            .setPositiveButton(activity.getString(R.string.notif_permission_rationale_allow)) { _, _ ->
                request(launcher)
            }
            .setNegativeButton(activity.getString(R.string.notif_permission_rationale_skip)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Shows a dialog directing the user to Settings when they have permanently denied the permission.
     */
    fun showSettingsRedirectDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.notif_permission_settings_title))
            .setMessage(activity.getString(R.string.notif_permission_settings_message))
            .setPositiveButton(activity.getString(R.string.notif_permission_settings_open)) { _, _ ->
                openAppNotificationSettings(activity)
            }
            .setNegativeButton(activity.getString(R.string.notif_permission_settings_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppNotificationSettings(activity: Activity) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        }
        activity.startActivity(intent)
    }
}
