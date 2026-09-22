package com.team.taskmanagementapp.pomodoro

import android.app.AlarmManager
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
import com.team.taskmanagementapp.TaskApplication
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.data.repository.PomodoroRepository
import com.team.taskmanagementapp.data.repository.PomodoroSettingsRepository
import com.team.taskmanagementapp.util.AlarmScheduler
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.NotificationHelper
import com.team.taskmanagementapp.util.NotificationPermissionManager
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
 *    action Pause / Resume / Skip / Stop. Notification dùng id âm
 *    ([com.team.taskmanagementapp.util.Constants.POMODORO_NOTIFICATION_ID]) để không bao
 *    giờ đụng id notification của task reminder (task.id luôn dương).
 *
 * ## Không drift
 * Nguồn thời gian duy nhất là `targetEndElapsedRealtime` do engine chốt khi bắt đầu phiên.
 * `tick()` chỉ **đọc lại** mốc đó; dù tick bị trễ (CPU sleep, Doze, app bị đẩy ra nền)
 * thì thời gian hiển thị vẫn đúng. Không có biến `seconds--` nào ở đây.
 *
 * ## Hết phiên: Sound / Vibration / Completion Notification
 * Khi một phiên chạy hết giờ, service sẽ:
 * 1. Đăng một **completion notification** trên channel cảnh báo riêng
 *    (`Constants.POMODORO_ALERT_CHANNEL_ID`, IMPORTANCE_HIGH) — chính channel này khiến
 *    hệ thống phát **sound + vibration** theo đúng cài đặt và tôn trọng Do Not Disturb.
 * 2. Chỉ khi KHÔNG thể đăng notification (người dùng từ chối `POST_NOTIFICATIONS` hoặc tắt
 *    notification của app) thì mới phát trực tiếp qua [PomodoroAlertPlayer].
 * Nhờ vậy không bao giờ kêu/rung 2 lần.
 *
 * Phiên bị Skip **không** phát cảnh báo (người dùng đang chủ động thao tác).
 *
 * ## Xử lý deep-sleep
 * FGS **không** tự giữ wakelock, và `delay()` trên main looper dựa trên `uptimeMillis`
 * (không tính thời gian CPU suspend). Nếu chỉ dựa vào vòng lặp tick thì khi màn hình tắt lâu
 * và thiết bị vào Doze, việc phát hiện "hết phiên" có thể bị trễ.
 *
 * Vì vậy mỗi khi có phiên chạy, service đặt thêm một alarm
 * `setExactAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP)` tại đúng mốc `targetEndElapsedRealtime`
 * (xem [syncSessionEndAlarm]) để đánh thức CPU ngay lúc phiên kết thúc.
 * ⚠️ Alarm này **không phải** nguồn thời gian: cơ chế `targetEndElapsedRealtime` +
 * `SystemClock.elapsedRealtime()` giữ nguyên hoàn toàn, alarm chỉ là tín hiệu đánh thức.
 * Mọi đường đi (ticker, alarm, lệnh từ notification) đều hội tụ về `engine.tick()`.
 *
 * Việc phát hiện kết thúc có thể đến đồng thời từ nhiều đường, nhưng Sound / Vibration /
 * Completion Notification chỉ phát **đúng một lần** nhờ [PomodoroCompletionTracker]
 * (khoá theo `PomodoroSnapshot.completionId`).
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

    /** Cấu hình Pomodoro người dùng đã lưu trong Settings (Task 11). */
    private val settingsRepository: PomodoroSettingsRepository by lazy {
        PomodoroSettingsRepository.from(this)
    }

    /** Bảo đảm mỗi phiên chỉ phát cảnh báo hết giờ đúng một lần. */
    private val completionTracker = PomodoroCompletionTracker()

    /** Ghi phiên đã hoàn thành + cộng dồn thống kê Task vào Room (Task 14). */
    private val pomodoroRepository: PomodoroRepository by lazy {
        PomodoroRepository(AppDatabase.getInstance(this).pomodoroDao())
    }

    private var serviceScope: CoroutineScope? = null
    private var tickerJob: Job? = null

    private var isForeground = false
    private var lastRenderedKey: String? = null

    private var sessionEndAlarmScheduled = false
    private var scheduledTargetEndElapsed = 0L

    // PendingIntent được tạo 1 lần cho mỗi vòng đời service (chúng là hằng số),
    // tránh phải tạo lại mỗi giây khi cập nhật notification.
    private lateinit var contentPendingIntent: PendingIntent
    private lateinit var pausePendingIntent: PendingIntent
    private lateinit var resumePendingIntent: PendingIntent
    private lateinit var skipPendingIntent: PendingIntent
    private lateinit var stopPendingIntent: PendingIntent
    private lateinit var sessionEndPendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()
        // Ticker chạy trên main thread: mọi thay đổi state của engine (tick, start, pause,
        // resume, skip, reset) đều nằm trên 1 luồng duy nhất nên không có race condition.
        // Khối lượng mỗi tick rất nhỏ (đọc elapsedRealtime + cập nhật notification 1 lần/giây).
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        NotificationHelper.createPomodoroChannel(this)
        NotificationHelper.createPomodoroAlertChannel(this)
        buildPendingIntents()
        // Service instance mới: xoá alarm cũ (nếu còn sót) và bỏ qua các phiên đã kết thúc
        // trước đó để không phát lại cảnh báo của phiên cũ.
        cancelSessionEndAlarm()
        completionTracker.syncTo(engine.currentSnapshot.completionId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Settings có thể đã thay đổi kể từ lần chạy trước -> nạp lại trước khi xử lý lệnh.
        // updateConfig() chỉ tác động tới phiên BẮT ĐẦU SAU ĐÓ: phiên đang chạy giữ nguyên
        // `targetEndElapsedRealtime` nên thời gian đếm ngược không bị lệch giữa phiên.
        engine.updateConfig(settingsRepository.load())

        if (intent?.action == ACTION_SESSION_END) {
            // AlarmManager đánh thức đúng lúc phiên kết thúc (kể cả khi máy đang Doze).
            // Không promoteToForeground: service vốn đã ở foreground vì phiên đang chạy.
            handleTick()
            if (engine.currentSnapshot.state == PomodoroTimerState.IDLE) {
                shutdownService()
            }
            return START_NOT_STICKY
        }

        applyAction(intent)

        if (engine.currentSnapshot.state == PomodoroTimerState.IDLE) {
            shutdownService()
            return START_NOT_STICKY
        }

        promoteToForeground()
        handleTick()
        syncTicker()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTicker()
        cancelSessionEndAlarm()
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
                handleTick()

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

    /**
     * Một "nhịp" xử lý duy nhất, dùng chung cho MỌI nguồn:
     * vòng lặp ticker, alarm đánh thức khi Doze, và mỗi lệnh đến từ notification.
     *
     * Nhờ gom về một chỗ nên việc phát hiện hết phiên và phát cảnh báo luôn nhất quán,
     * không phụ thuộc việc nhịp nào chạy trước.
     */
    private fun handleTick() {
        engine.tick()
        alertIfSessionJustFinished()
        syncSessionEndAlarm()
        renderNotification()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Sound / Vibration / Completion Notification khi hết phiên
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Phát cảnh báo nếu vừa có một phiên chạy hết giờ.
     *
     * [PomodoroCompletionTracker] bảo đảm dù hàm này được gọi ở mọi nhịp tick thì
     * mỗi phiên chỉ phát đúng một lần; phiên bị Skip sẽ không phát.
     */
    private fun alertIfSessionJustFinished() {
        val record = completionTracker.consumeCompletedSession(engine.currentSnapshot) ?: return

        // Lưu phiên TRƯỚC khi báo cho người dùng: tracker đã bảo đảm mỗi phiên
        // (theo `completionId`) chỉ đi qua đây đúng một lần, nên không thể sinh
        // bản ghi trùng hoặc cộng dồn thống kê 2 lần dù tick đến từ ticker,
        // alarm Doze hay lệnh trên notification.
        persistCompletedSession(record)

        showCompletionNotification(record)

        if (!canPostNotifications()) {
            // Không đăng được notification => channel cảnh báo không thể kêu/rung,
            // nên phát trực tiếp. Chỉ một trong hai đường được chạy nên không kêu 2 lần.
            PomodoroAlertPlayer.play(this)
        }
    }

    /**
     * Lưu phiên vừa chạy hết giờ vào `pomodoro_sessions` và cộng dồn thống kê Task.
     *
     * Chạy trên [TaskApplication.applicationScope] thay vì `serviceScope`:
     * người dùng có thể bấm Stop ngay sau khi phiên kết thúc, service bị huỷ và
     * transaction đang dở sẽ bị cancel → mất phiên. Scope của Application sống cùng
     * process nên bản ghi luôn hoàn tất.
     *
     * Lỗi ghi (ví dụ Task đã bị xoá → DAO trả `false`) chỉ được ghi log, không được
     * làm chết service hay ném exception ra ngoài.
     */
    private fun persistCompletedSession(record: CompletedSessionRecord) {
        val scope = (application as? TaskApplication)?.applicationScope ?: return
        scope.launch {
            runCatching { pomodoroRepository.recordCompletedSession(record) }
                .onFailure { Log.w(TAG, "Unable to persist completed Pomodoro session", it) }
        }
    }

    /**
     * Notification báo hết phiên (kèm sound + vibration do channel cảnh báo đảm nhiệm).
     *
     * Dùng id [Constants.POMODORO_COMPLETION_NOTIFICATION_ID] — vẫn là số âm nên không thể
     * đè lên notification nhắc việc của task (task.id luôn dương).
     */
    private fun showCompletionNotification(record: CompletedSessionRecord) {
        val snapshot = engine.currentSnapshot
        // Nếu phiên kế tiếp đã auto-start thì nó chính là sessionType đang chạy;
        // còn nếu đang chờ người dùng bấm thì lấy nextSessionType.
        val nextType = if (snapshot.state == PomodoroTimerState.RUNNING) {
            snapshot.sessionType
        } else {
            snapshot.nextSessionType
        }
        val nextMinutes = snapshot.config.durationMinutesFor(nextType)

        val notification = NotificationCompat.Builder(this, Constants.POMODORO_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pomodoro)
            .setContentTitle(
                getString(
                    R.string.pomodoro_completion_title,
                    getString(sessionLabelRes(record.sessionType))
                )
            )
            .setContentText(
                getString(
                    R.string.pomodoro_completion_body,
                    getString(sessionLabelRes(nextType)),
                    nextMinutes
                )
            )
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_play,
                getString(R.string.pomodoro_action_start_next),
                resumePendingIntent
            )
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.pomodoro_action_stop),
                stopPendingIntent
            )
            .build()

        runCatching {
            NotificationManagerCompat.from(this)
                .notify(Constants.POMODORO_COMPLETION_NOTIFICATION_ID, notification)
        }.onFailure {
            Log.w(TAG, "Unable to post Pomodoro completion notification", it)
        }
    }

    /** App có thể đăng notification hay không (quyền runtime + công tắc trong Settings). */
    private fun canPostNotifications(): Boolean =
        NotificationPermissionManager.isGranted(this) &&
            runCatching { NotificationManagerCompat.from(this).areNotificationsEnabled() }
                .getOrDefault(false)

    // ══════════════════════════════════════════════════════════════════════════
    // Alarm đánh thức đúng mốc kết thúc phiên (chống trễ do Doze)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Đồng bộ alarm với phiên đang chạy.
     *
     * Alarm chỉ có ý nghĩa khi phiên đang RUNNING (lúc đó mới có mốc kết thúc); mọi trạng
     * thái khác (PAUSED / COMPLETED / IDLE) đều huỷ alarm để không đánh thức vô ích.
     * Chỉ đặt lại khi mốc kết thúc thực sự đổi, tránh gọi AlarmManager mỗi giây.
     */
    private fun syncSessionEndAlarm() {
        val snapshot = engine.currentSnapshot
        val target = snapshot.targetEndElapsedRealtime
        val shouldSchedule = snapshot.state == PomodoroTimerState.RUNNING && target > 0L

        if (!shouldSchedule) {
            cancelSessionEndAlarm()
            return
        }
        if (sessionEndAlarmScheduled && target == scheduledTargetEndElapsed) return

        cancelSessionEndAlarm()
        scheduledTargetEndElapsed = target
        sessionEndAlarmScheduled = true
        scheduleSessionEndAlarm(target)
    }

    private fun cancelSessionEndAlarm() {
        scheduledTargetEndElapsed = 0L
        sessionEndAlarmScheduled = false
        runCatching { alarmManager()?.cancel(sessionEndPendingIntent) }.onFailure {
            Log.w(TAG, "Unable to cancel Pomodoro session end alarm", it)
        }
    }

    /**
     * Đặt alarm đánh thức CPU ngay tại mốc phiên kết thúc.
     *
     * Dùng `ELAPSED_REALTIME_WAKEUP` vì khớp trực tiếp với `targetEndElapsedRealtime`
     * (cùng gốc `elapsedRealtime`, không bị ảnh hưởng khi người dùng đổi giờ hệ thống).
     * `*AndAllowWhileIdle` để alarm vẫn nổ khi thiết bị đang trong Doze.
     *
     * Ưu tiên exact alarm; nếu người dùng chưa cấp quyền (Android 12+) hoặc hệ thống từ chối
     * thì lùi về inexact — vẫn đánh thức được máy, chỉ kém chính xác vài phút. Vòng lặp tick
     * vẫn là đường chính khi CPU đang thức, nên đây chỉ là lưới an toàn.
     */
    private fun scheduleSessionEndAlarm(targetEndElapsed: Long) {
        val manager = alarmManager() ?: return
        val triggerAt = targetEndElapsed.coerceAtLeast(SystemClock.elapsedRealtime() + 1L)

        if (AlarmScheduler.canScheduleExactAlarms(this)) {
            val exactScheduled = runCatching {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    sessionEndPendingIntent
                )
            }.onFailure {
                Log.w(TAG, "Exact session end alarm rejected; falling back to inexact", it)
            }.isSuccess

            if (exactScheduled) return
        }

        runCatching {
            manager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                sessionEndPendingIntent
            )
        }.onFailure {
            Log.w(TAG, "Unable to schedule Pomodoro session end alarm", it)
        }
    }

    private fun alarmManager(): AlarmManager? =
        runCatching { getSystemService(Context.ALARM_SERVICE) as? AlarmManager }
            .onFailure { Log.w(TAG, "Alarm service unavailable", it) }
            .getOrNull()

    // ══════════════════════════════════════════════════════════════════════════
    // Notification
    // ══════════════════════════════════════════════════════════════════════════

    private fun renderNotification() {
        // Không có phiên nào -> không có notification ongoing để hiển thị.
        if (engine.currentSnapshot.state == PomodoroTimerState.IDLE) return

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

        // Alarm đánh thức gửi thẳng vào service bằng chính PendingIntent này, nên phải khớp
        // đúng action + request code ở cả lúc đặt và lúc huỷ alarm.
        sessionEndPendingIntent = servicePendingIntent(
            ACTION_SESSION_END,
            Constants.POMODORO_SESSION_END_REQUEST_CODE,
            flags
        )
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

        /**
         * Chỉ dùng nội bộ: [android.app.AlarmManager] gửi action này để đánh thức service
         * đúng mốc phiên kết thúc khi thiết bị đang Doze/ngủ sâu.
         */
        const val ACTION_SESSION_END = "com.team.taskmanagementapp.action.POMODORO_SESSION_END"

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
