package com.team.taskmanagementapp.pomodoro

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.team.taskmanagementapp.MainActivity
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service chạy Pomodoro Timer.
 *
 * ## Vai trò
 * Service này **không chứa logic đếm giờ** — logic nằm ở [PomodoroTimerEngine] do
 * [PomodoroTimerController] giữ. Service chỉ làm 3 việc:
 * 1. Giữ app ở trạng thái foreground-service để Android không đình chỉ timer khi app
 *    xuống background hoặc màn hình tắt.
 * 2. `tick()` engine mỗi giây, canh đúng mốc giây của [SystemClock.elapsedRealtime].
 * 3. Vẽ notification ongoing (session type + countdown realtime + trạng thái) kèm
 *    action Pause / Resume / Skip / Stop.
 *
 * ## Không drift
 * Nguồn thời gian duy nhất là `targetEndElapsedRealtime` do engine chốt khi bắt đầu phiên.
 * `tick()` chỉ **đọc lại** mốc đó; dù tick bị trễ (CPU sleep, Doze, app bị đẩy ra nền)
 * thì thời gian hiển thị vẫn đúng. Không có biến `seconds--` nào ở đây.
 *
 * ## Chống recreate
 * Timer không phụ thuộc Activity/Fragment. UI observe [PomodoroTimerController.state];
 * rotate màn hình không tạo timer mới, không làm sai lệch thời gian.
 *
 * ## Vòng đời
 * - Service trả về [START_NOT_STICKY]: nếu process bị kill, hệ thống **không** tự dựng lại
 *   service (tránh việc `startForeground()` từ background bị chặn trên Android 12+).
 *   Task recovery sau này sẽ dựng lại timer từ state đã persist rồi gọi lại
 *   [start] — xem `PomodoroTimerController.install`.
 * - Trong [onDestroy], nếu phiên đang chạy thì engine được `pause()` để không tồn tại
 *   trạng thái "timer chạy ảo" (state RUNNING nhưng không ai tick).
 *
 * ## Threading
 * [PomodoroTimerEngine] là state machine đơn luồng (không synchronized). Vì vậy ticker và
 * mọi lệnh điều khiển (`onStartCommand`) đều chạy trên **main thread** — nhờ đó các phép
 * đọc-sửa-ghi state không bao giờ chồng lên nhau.
 */
class PomodoroService : Service() {

    private val engine: PomodoroTimerEngine
        get() = PomodoroTimerController.engine

    private var serviceScope: CoroutineScope? = null
    private var tickerJob: Job? = null

    private var isForeground = false
    private var lastRenderedKey: String? = null

    // PendingIntent được tạo 1 lần cho mỗi vòng đời service (chúng là hằng số),
    // tránh phải tạo lại mỗi giây khi cập nhật notification.
    private lateinit var contentPendingIntent: PendingIntent
    private lateinit var pausePendingIntent: PendingIntent
    private lateinit var resumePendingIntent: PendingIntent
    private lateinit var skipPendingIntent: PendingIntent
    private lateinit var stopPendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()
        // Ticker chạy trên main thread: mọi thay đổi state của engine (tick, start, pause,
        // resume, skip, reset) đều nằm trên 1 luồng duy nhất nên không có race condition.
        // Khối lượng mỗi tick rất nhỏ (đọc elapsedRealtime + cập nhật notification 1 lần/giây).
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        NotificationHelper.createPomodoroChannel(this)
        buildPendingIntents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        applyAction(intent)

        if (engine.currentSnapshot.state == PomodoroTimerState.IDLE) {
            shutdownService()
            return START_NOT_STICKY
        }

        promoteToForeground()
        syncTicker()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTicker()
        // Không để lại trạng thái RUNNING "ảo" khi service bị hệ thống huỷ.
        if (engine.currentSnapshot.state == PomodoroTimerState.RUNNING) {
            engine.pause()
        }
        isForeground = false
        lastRenderedKey = null
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Xử lý lệnh
    // ══════════════════════════════════════════════════════════════════════════

    private fun applyAction(intent: Intent?) {
        when (intent?.action) {
            ACTION_START -> {
                val requestedTaskId = intent.getLongExtra(
                    Constants.EXTRA_POMODORO_TASK_ID,
                    Constants.NO_TASK_ID
                ).takeIf { it >= 0L }
                // IDLE -> bắt đầu FOCUS mới; COMPLETED -> bắt đầu phiên kế tiếp
                engine.start(requestedTaskId)
            }

            ACTION_PAUSE -> engine.pause()
            ACTION_RESUME -> engine.resume()
            ACTION_SKIP -> engine.skip()
            ACTION_STOP -> engine.reset()

            else -> Log.d(TAG, "onStartCommand without a known action (intent=$intent)")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Foreground
    // ══════════════════════════════════════════════════════════════════════════

    private fun promoteToForeground() {
        val notification = buildNotification()
        lastRenderedKey = notificationKey(engine.currentSnapshot)
        startForeground(Constants.POMODORO_NOTIFICATION_ID, notification)
        isForeground = true
    }

    /**
     * Dừng service an toàn.
     *
     * Nếu service vừa được tạo bằng `Context.startForegroundService()` mà chưa từng vào
     * foreground, phải gọi [startForeground] một lần để thoả hợp đồng "vào foreground
     * trong ~5 giây" — nếu không Android sẽ ném
     * `Context.startForegroundService() did not then call Service.startForeground()`.
     * Trường hợp thông thường (service đang foreground rồi mới Stop) thì bỏ qua bước này
     * để không nháy một notification "Đã dừng".
     */
    private fun shutdownService() {
        stopTicker()
        lastRenderedKey = null
        if (!isForeground) {
            runCatching { startForeground(Constants.POMODORO_NOTIFICATION_ID, buildNotification()) }
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Ticker
    // ══════════════════════════════════════════════════════════════════════════

    private fun syncTicker() {
        if (engine.currentSnapshot.state == PomodoroTimerState.RUNNING) {
            startTicker()
        } else {
            stopTicker()
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return

        tickerJob = serviceScope?.launch {
            while (isActive) {
                engine.tick()
                renderNotification()

                // Phiên kết thúc và không auto-start -> dừng ticker (state = COMPLETED),
                // service + notification vẫn sống để người dùng bắt đầu phiên kế tiếp.
                if (engine.currentSnapshot.state != PomodoroTimerState.RUNNING) break

                delay(millisUntilNextTick())
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    /** Canh tick đúng vào mốc giây của `elapsedRealtime` để countdown nhảy đều. */
    private fun millisUntilNextTick(): Long {
        val now = SystemClock.elapsedRealtime()
        return TICK_INTERVAL_MS - (now % TICK_INTERVAL_MS)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Notification
    // ══════════════════════════════════════════════════════════════════════════

    private fun renderNotification() {
        val snapshot = engine.currentSnapshot
        val key = notificationKey(snapshot)
        if (key == lastRenderedKey) return

        lastRenderedKey = key
        runCatching {
            NotificationManagerCompat.from(this)
                .notify(Constants.POMODORO_NOTIFICATION_ID, buildNotification(snapshot))
        }.onFailure {
            // Ví dụ POST_NOTIFICATIONS bị từ chối trên Android 13+: service vẫn chạy,
            // chỉ là người dùng không thấy notification.
            Log.w(TAG, "Unable to update Pomodoro notification", it)
        }
    }

    /** Chỉ cập nhật notification khi nội dung hiển thị thực sự thay đổi. */
    private fun notificationKey(snapshot: PomodoroSnapshot): String = buildString {
        append(snapshot.state).append('|')
        append(snapshot.sessionType).append('|')
        append(snapshot.nextSessionType).append('|')
        append(snapshot.formattedRemaining).append('|')
        append(snapshot.currentCycle).append('/').append(snapshot.totalCycles)
    }

    private fun buildNotification(
        snapshot: PomodoroSnapshot = engine.currentSnapshot
    ): Notification {
        val contentTitle = getString(
            R.string.pomodoro_notification_title,
            getString(sessionLabelRes(snapshot.sessionType)),
            snapshot.currentCycle,
            snapshot.totalCycles
        )

        val contentText = when (snapshot.state) {
            PomodoroTimerState.COMPLETED -> getString(
                R.string.pomodoro_notification_next_session,
                getString(sessionLabelRes(snapshot.nextSessionType))
            )

            PomodoroTimerState.IDLE -> getString(R.string.pomodoro_notification_idle)

            else -> getString(
                R.string.pomodoro_notification_time_status,
                snapshot.formattedRemaining,
                getString(statusRes(snapshot.state))
            )
        }

        val builder = NotificationCompat.Builder(this, Constants.POMODORO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pomodoro)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (snapshot.state != PomodoroTimerState.IDLE && snapshot.totalMillis > 0L) {
            builder.setProgress(
                PROGRESS_MAX,
                (snapshot.progressFraction * PROGRESS_MAX).toInt().coerceIn(0, PROGRESS_MAX),
                false
            )
        }

        when (snapshot.state) {
            PomodoroTimerState.RUNNING -> builder.addAction(
                R.drawable.ic_pause,
                getString(R.string.pomodoro_action_pause),
                pausePendingIntent
            )

            PomodoroTimerState.PAUSED -> builder.addAction(
                R.drawable.ic_play,
                getString(R.string.pomodoro_action_resume),
                resumePendingIntent
            )

            PomodoroTimerState.COMPLETED -> builder.addAction(
                R.drawable.ic_play,
                getString(R.string.pomodoro_action_start_next),
                resumePendingIntent
            )

            PomodoroTimerState.IDLE -> Unit
        }

        if (snapshot.state != PomodoroTimerState.IDLE) {
            builder.addAction(
                R.drawable.ic_skip_next,
                getString(R.string.pomodoro_action_skip),
                skipPendingIntent
            )
            builder.addAction(
                R.drawable.ic_stop,
                getString(R.string.pomodoro_action_stop),
                stopPendingIntent
            )
        }

        return builder.build()
    }

    private fun sessionLabelRes(sessionType: SessionType): Int = when (sessionType) {
        SessionType.FOCUS -> R.string.pomodoro_session_focus
        SessionType.SHORT_BREAK -> R.string.pomodoro_session_short_break
        SessionType.LONG_BREAK -> R.string.pomodoro_session_long_break
    }

    private fun statusRes(state: PomodoroTimerState): Int = when (state) {
        PomodoroTimerState.RUNNING -> R.string.pomodoro_status_running
        PomodoroTimerState.PAUSED -> R.string.pomodoro_status_paused
        PomodoroTimerState.COMPLETED -> R.string.pomodoro_status_completed
        PomodoroTimerState.IDLE -> R.string.pomodoro_notification_idle
    }

    private fun buildPendingIntents() {
        val flags = PendingIntent.FLAG_IMMUTABLE

        contentPendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(Constants.EXTRA_OPEN_POMODORO_TIMER, true)
            },
            flags
        )

        pausePendingIntent = servicePendingIntent(ACTION_PAUSE, REQUEST_PAUSE, flags)
        resumePendingIntent = servicePendingIntent(ACTION_RESUME, REQUEST_RESUME, flags)
        skipPendingIntent = servicePendingIntent(ACTION_SKIP, REQUEST_SKIP, flags)
        stopPendingIntent = servicePendingIntent(ACTION_STOP, REQUEST_STOP, flags)
    }

    private fun servicePendingIntent(action: String, requestCode: Int, flags: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, PomodoroService::class.java).setAction(action),
            flags
        )

    companion object {
        private const val TAG = "PomodoroService"

        const val ACTION_START = "com.team.taskmanagementapp.action.POMODORO_START"
        const val ACTION_PAUSE = "com.team.taskmanagementapp.action.POMODORO_PAUSE"
        const val ACTION_RESUME = "com.team.taskmanagementapp.action.POMODORO_RESUME"
        const val ACTION_SKIP = "com.team.taskmanagementapp.action.POMODORO_SKIP"
        const val ACTION_STOP = "com.team.taskmanagementapp.action.POMODORO_STOP"

        private const val TICK_INTERVAL_MS = 1_000L
        private const val PROGRESS_MAX = 100

        private const val REQUEST_OPEN = 1
        private const val REQUEST_PAUSE = 2
        private const val REQUEST_RESUME = 3
        private const val REQUEST_SKIP = 4
        private const val REQUEST_STOP = 5

        /**
         * Bắt đầu timer (từ IDLE) hoặc chuyển sang phiên kế tiếp (từ COMPLETED).
         *
         * @param taskId task gắn với phiên tập trung, null nếu tập trung không gắn task
         */
        fun start(context: Context, taskId: Long? = null) {
            val intent = Intent(context, PomodoroService::class.java)
                .setAction(ACTION_START)
                .putExtra(Constants.EXTRA_POMODORO_TASK_ID, taskId ?: Constants.NO_TASK_ID)
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) = sendAction(context, ACTION_PAUSE)

        fun resume(context: Context) = sendAction(context, ACTION_RESUME)

        fun skip(context: Context) = sendAction(context, ACTION_SKIP)

        /** Reset/Stop: đưa timer về IDLE và hạ service xuống. */
        fun stop(context: Context) = sendAction(context, ACTION_STOP)

        private fun sendAction(context: Context, action: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, PomodoroService::class.java).setAction(action)
            )
        }
    }
}
