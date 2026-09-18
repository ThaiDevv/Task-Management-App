package com.team.taskmanagementapp.pomodoro

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.team.taskmanagementapp.util.NotificationHelper

/**
 * Phát âm thanh + rung khi phiên Pomodoro kết thúc — **chỉ dùng làm đường dự phòng**.
 *
 * ## Đường đi chính
 * Bình thường âm thanh và rung do **channel cảnh báo** đảm nhiệm
 * (`NotificationHelper.createPomodoroAlertChannel()` — IMPORTANCE_HIGH, có sound + vibration).
 * Cách đó đúng chuẩn Android: hệ thống tự phát, tự tôn trọng Do Not Disturb, chế độ im lặng
 * và lựa chọn channel của người dùng, đồng thời không bao giờ kêu 2 lần.
 *
 * ## Vì sao vẫn cần lớp này
 * Nếu người dùng từ chối quyền `POST_NOTIFICATIONS` (Android 13+) hoặc tắt notification của
 * app thì notification cảnh báo **không hiển thị được**, kéo theo việc channel không thể phát
 * sound/vibration ⇒ phiên sẽ kết thúc trong im lặng. [play] chỉ được gọi trong đúng tình huống
 * đó, nên tại một thời điểm luôn chỉ có MỘT cơ chế phát (không kêu/rung trùng).
 */
object PomodoroAlertPlayer {

    private const val TAG = "PomodoroAlertPlayer"

    /** Phát rung rồi phát âm thanh báo hết giờ. */
    fun play(context: Context) {
        playVibration(context)
        playSound(context)
    }

    fun playVibration(context: Context) {
        val vibrator = vibrator(context) ?: return

        runCatching {
            // minSdk 26 nên VibrationEffect luôn khả dụng.
            // Repetition index -1 = chỉ rung đúng pattern một lần.
            vibrator.vibrate(
                VibrationEffect.createWaveform(NotificationHelper.POMODORO_VIBRATION_PATTERN, -1)
            )
        }.onFailure {
            Log.w(TAG, "Unable to vibrate for Pomodoro session end", it)
        }
    }

    fun playSound(context: Context) {
        runCatching {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            // Ringtone.play() phát đúng một lần (không lặp) rồi tự dừng.
            RingtoneManager.getRingtone(context, soundUri)?.play()
        }.onFailure {
            Log.w(TAG, "Unable to play sound for Pomodoro session end", it)
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
