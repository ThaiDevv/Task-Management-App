package com.team.taskmanagementapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.repository.PomodoroSettingsRepository
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.pomodoro.PomodoroService
import com.team.taskmanagementapp.pomodoro.PomodoroSnapshot
import com.team.taskmanagementapp.pomodoro.PomodoroTimerController
import com.team.taskmanagementapp.pomodoro.PomodoroTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * TaskId mà UI phải hiển thị.
 *
 * **Phiên đang có là nguồn sự thật**: khi timer đã gắn với một công việc
 * (RUNNING/PAUSED/COMPLETED) thì công việc đó luôn được hiển thị, kể cả khi lựa chọn chờ
 * trong ViewModel khác hoặc đã bị xoá. Khi timer IDLE (chưa chạy hoặc vừa Stop) thì dùng
 * lựa chọn người dùng đã chọn cho phiên sắp tới.
 *
 * Tách thành hàm thuần để kiểm tra được trên JVM.
 */
internal fun resolveDisplayedTaskId(
    snapshot: PomodoroSnapshot,
    pendingTaskId: Long?
): Long? = snapshot.taskId ?: pendingTaskId

/** Kết quả quyết định khi có taskId được truyền vào từ Task Detail (Task 13). */
internal enum class TaskIdArgumentDecision {
    /** TaskId trùng với công việc đang hiển thị — không cần làm gì (ví dụ rotate màn hình). */
    ALREADY_SELECTED,

    /** Đang có phiên Pomodoro (RUNNING/PAUSED/COMPLETED) — giữ nguyên, KHÔNG đổi công việc. */
    SESSION_ACTIVE,

    /** Timer đang IDLE — cần tra cứu công việc trong DB rồi chọn. */
    LOOKUP_TASK
}

/**
 * Quyết định xử lý taskId truyền từ Task Detail.
 *
 * Tách thành hàm thuần để kiểm tra được trên JVM — đây là quy tắc bảo vệ phiên đang chạy:
 * khi đã có phiên cho một công việc khác, taskId mới **không** được ghi đè.
 */
internal fun decideTaskIdArgument(
    snapshot: PomodoroSnapshot,
    pendingTaskId: Long?,
    requestedTaskId: Long
): TaskIdArgumentDecision = when {
    resolveDisplayedTaskId(snapshot, pendingTaskId) == requestedTaskId ->
        TaskIdArgumentDecision.ALREADY_SELECTED

    snapshot.state != PomodoroTimerState.IDLE ->
        TaskIdArgumentDecision.SESSION_ACTIVE

    else -> TaskIdArgumentDecision.LOOKUP_TASK
}

/** Thông báo một lần cho UI khi thao tác từ Task Detail không thể áp dụng. */
enum class PomodoroNotice {
    /** taskId truyền vào không còn tồn tại trong DB (đã bị xoá). */
    TASK_NOT_FOUND,

    /** Đang có phiên Pomodoro của công việc khác nên không đổi được. */
    SESSION_ALREADY_ACTIVE
}

/**
 * ViewModel cho Pomodoro Timer Screen (Task 9 + Task 10).
 *
 * ViewModel này **không** giữ timer, không đếm ngược, không sao chép logic của
 * `PomodoroService`. Nó làm 3 việc:
 * 1. Expose `StateFlow<PomodoroSnapshot>` của [PomodoroTimerController] cho UI observe —
 *    đúng cùng nguồn state mà Service đang cập nhật mỗi giây.
 * 2. Giữ **lựa chọn công việc** cho phiên sắp bắt đầu và cho biết công việc đó còn tồn tại hay không.
 * 3. Chuyển thao tác của người dùng thành lệnh cho [PomodoroService]
 *    (start/pause/resume/skip/stop). Không tự gọi engine, để Service luôn là nơi duy nhất
 *    điều khiển timer và giữ foreground notification đồng bộ.
 */
class PomodoroViewModel(
    private val taskRepository: TaskRepository,
    private val settingsRepository: PomodoroSettingsRepository,
    private val appContext: Context
) : ViewModel() {

    /** TaskId người dùng đã chọn cho phiên sắp tới (null = chưa chọn). */
    private val selectedTaskId = MutableStateFlow<Long?>(null)

    /**
     * Danh sách công việc hiện có, dùng để tra cứu công việc đang chọn.
     * Giữ nóng ngay từ khi tạo ViewModel để không có khoảng trống dữ liệu lúc người dùng thao tác.
     */
    private val tasks: StateFlow<List<Task>> = taskRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Công việc đang được hiển thị (đang tập trung), hoặc `null` nếu chưa chọn / công việc
     * đã bị xoá. UI chỉ cần observe flow này, không phải tự tra cứu.
     */
    val selectedTask: StateFlow<Task?> = combine(
        PomodoroTimerController.state,
        selectedTaskId
    ) { snapshot, pendingTaskId ->
        resolveDisplayedTaskId(snapshot, pendingTaskId)
    }.combine(tasks) { displayedId, list ->
        displayedId?.let { id -> list.firstOrNull { it.id.toLong() == id } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Nếu công việc đang chọn bị xoá ở nơi khác thì tự bỏ chọn, tránh truyền taskId "chết"
        // xuống Service (bảng pomodoro_sessions có khoá ngoại NOT NULL tới tasks).
        viewModelScope.launch {
            tasks.collect { list ->
                // Danh sách rỗng có thể chỉ là giá trị khởi tạo của flow -> không xoá lựa chọn.
                if (list.isEmpty()) return@collect

                val selected = selectedTaskId.value ?: return@collect
                if (list.none { it.id.toLong() == selected }) {
                    selectedTaskId.value = null
                }
            }
        }
    }

    /** State của timer. Dùng getter để luôn trỏ tới engine hiện hành. */
    val uiState: StateFlow<PomodoroSnapshot>
        get() = PomodoroTimerController.state

    val currentSelectedTaskId: Long?
        get() = selectedTaskId.value

    private val _notice = MutableStateFlow<PomodoroNotice?>(null)

    /**
     * Thông báo cần hiển thị cho người dùng (đã ở dạng state nên không mất khi rotate),
     * UI hiển thị xong phải gọi [onNoticeShown] để không hiện lại.
     */
    val notice: StateFlow<PomodoroNotice?> = _notice.asStateFlow()

    fun onNoticeShown() {
        _notice.value = null
    }

    /**
     * Áp dụng taskId được truyền từ Task Detail (Task 13).
     *
     * - Nếu trùng công việc đang hiển thị: không làm gì (tránh báo lỗi khi rotate màn hình).
     * - Nếu đang có phiên Pomodoro: giữ nguyên phiên, chỉ thông báo — **không** đổi taskId.
     * - Nếu IDLE nhưng công việc không còn tồn tại: thông báo và giữ nguyên luồng chọn tay.
     */
    fun onTaskIdProvidedFromDetail(taskId: Long) {
        viewModelScope.launch {
            val snapshot = PomodoroTimerController.snapshot

            when (decideTaskIdArgument(snapshot, selectedTaskId.value, taskId)) {
                TaskIdArgumentDecision.ALREADY_SELECTED -> Unit

                TaskIdArgumentDecision.SESSION_ACTIVE ->
                    _notice.value = PomodoroNotice.SESSION_ALREADY_ACTIVE

                TaskIdArgumentDecision.LOOKUP_TASK -> {
                    val task = taskRepository.getTaskById(taskId)
                    if (task == null) {
                        _notice.value = PomodoroNotice.TASK_NOT_FOUND
                    } else {
                        selectedTaskId.value = taskId
                    }
                }
            }
        }
    }

    /**
     * Nạp lại cấu hình Pomodoro mới nhất từ Settings vào engine.
     *
     * Gọi khi màn hình Pomodoro hiển thị lại: nếu người dùng vừa đổi thời lượng ở
     * `PomodoroSettingsFragment` thì phần xem trước đồng hồ (trạng thái IDLE) cập nhật ngay.
     * Việc này chỉ đổi cấu hình cho phiên **bắt đầu sau** — `PomodoroTimerEngine.updateConfig`
     * không đụng tới `targetEndElapsedRealtime` của phiên đang chạy.
     */
    fun syncSettings() {
        PomodoroTimerController.engine.updateConfig(settingsRepository.load())
    }

    /** Ghi nhận công việc người dùng vừa chọn trong bottom sheet. */
    fun onTaskSelected(taskId: Long) {
        selectedTaskId.value = taskId
    }

    /**
     * Chỉ được đổi công việc khi chưa có phiên nào chạy: đang RUNNING/PAUSED thì công việc
     * đã gắn với phiên hiện tại, còn COMPLETED thì chuỗi phiên vẫn tiếp tục cùng công việc đó.
     */
    fun canSelectTask(snapshot: PomodoroSnapshot): Boolean =
        snapshot.state == PomodoroTimerState.IDLE

    /**
     * Như [canSelectTask] nhưng đọc state hiện tại của timer, dùng cho các sự kiện UI
     * (ví dụ: người dùng chạm vào thẻ công việc).
     */
    fun canSelectTask(): Boolean = canSelectTask(PomodoroTimerController.snapshot)

    /**
     * Nút hành động chính: IDLE → "Bắt đầu", PAUSED → "Tiếp tục", COMPLETED → "Phiên tiếp theo".
     *
     * @return `false` nếu chưa chọn công việc (UI cần mở Task Selector trước).
     *         Mọi phiên tập trung đều phải gắn với một công việc có thật trong DB.
     */
    fun onPrimaryAction(): Boolean {
        val snapshot = PomodoroTimerController.snapshot
        return when (snapshot.state) {
            PomodoroTimerState.IDLE -> {
                val taskId = selectedTaskId.value
                if (taskId == null) {
                    false
                } else {
                    PomodoroService.start(appContext, taskId)
                    true
                }
            }

            PomodoroTimerState.COMPLETED -> {
                // Engine giữ nguyên công việc của chuỗi phiên hiện tại (taskId truyền vào bị bỏ qua),
                // nên chỉ cần chuyển sang phiên kế tiếp.
                PomodoroService.start(appContext, selectedTaskId.value)
                true
            }

            PomodoroTimerState.PAUSED -> {
                PomodoroService.resume(appContext)
                true
            }

            // Đang chạy thì nút chính đã bị disable trên UI.
            PomodoroTimerState.RUNNING -> true
        }
    }

    fun onPauseClicked() {
        PomodoroService.pause(appContext)
    }

    fun onSkipClicked() {
        PomodoroService.skip(appContext)
    }

    fun onStopClicked() {
        PomodoroService.stop(appContext)
    }
}
