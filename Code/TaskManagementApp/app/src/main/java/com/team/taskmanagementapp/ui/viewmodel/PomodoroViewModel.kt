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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
     * Công việc đang được chọn, hoặc `null` nếu chưa chọn / công việc đã bị xoá.
     * UI chỉ cần observe flow này, không phải tự tra cứu.
     */
    val selectedTask: StateFlow<Task?> = combine(tasks, selectedTaskId) { list, id ->
        id?.let { selectedId -> list.firstOrNull { it.id.toLong() == selectedId } }
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
