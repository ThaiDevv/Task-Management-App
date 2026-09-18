package com.team.taskmanagementapp.ui.pomodoro

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.data.repository.PomodoroSettingsRepository
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentPomodoroBinding
import com.team.taskmanagementapp.pomodoro.PomodoroSnapshot
import com.team.taskmanagementapp.pomodoro.PomodoroTimerState
import com.team.taskmanagementapp.ui.viewmodel.PomodoroViewModel
import com.team.taskmanagementapp.ui.viewmodel.PomodoroViewModelFactory
import com.team.taskmanagementapp.util.Constants
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Pomodoro Timer Screen (Task 9) + Task Selector & Cycle Indicator (Task 10).
 *
 * ## Nguyên tắc
 * Fragment này **chỉ hiển thị**. Nó không tạo Handler / CountDownTimer / biến `seconds--`,
 * không tính toán thời gian, không giữ cycle counter riêng. Toàn bộ dữ liệu đến từ
 * [PomodoroViewModel]: timer từ `PomodoroTimerController` (cùng nguồn state mà
 * `PomodoroService` cập nhật mỗi giây), công việc đang chọn từ `TaskRepository`.
 *
 * ## Cycle Indicator
 * Số chấm được tô lấy trực tiếp từ `PomodoroSnapshot.focusSessionsInCurrentSet` — số phiên
 * FOCUS đã hoàn thành trong chu kỳ hiện tại do state machine đếm. UI không tự đếm:
 * `○ ○ ○ ○` → hoàn thành Focus 1 → `● ○ ○ ○` → … → hoàn thành Focus 4 → `● ● ● ●` → Long Break.
 *
 * ## Task Selector
 * Công việc lấy từ Room qua bottom sheet [PomodoroTaskSelectorBottomSheet]; bottom sheet trả
 * kết quả bằng Fragment Result API. Khi bắt đầu một phiên, `taskId` của công việc đang chọn
 * được truyền xuống Service (không hard-code, không dữ liệu mẫu).
 */
class PomodoroFragment : Fragment() {

    private var _binding: FragmentPomodoroBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: PomodoroViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext().applicationContext)
        PomodoroViewModelFactory(
            TaskRepository(database.taskDao()),
            PomodoroSettingsRepository.from(requireContext()),
            requireContext()
        )
    }

    /** Các chấm cycle được dựng động vì số chu kỳ có thể cấu hình (mặc định 4). */
    private val cycleDots = mutableListOf<View>()
    private var builtCycleCount = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPomodoroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupTaskSelectorResult()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cycleDots.clear()
        builtCycleCount = -1
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // Cấu hình có thể vừa được đổi ở Pomodoro Settings -> đồng bộ để phần xem trước
        // thời lượng (trạng thái IDLE) hiển thị đúng ngay khi quay lại màn hình này.
        viewModel.syncSettings()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnPomodoroSettings.setOnClickListener {
            findNavController().navigate(R.id.pomodoroSettingsFragment)
        }
        binding.btnPrimary.setOnClickListener { handlePrimaryAction() }
        binding.btnPause.setOnClickListener { viewModel.onPauseClicked() }
        binding.btnSkip.setOnClickListener { viewModel.onSkipClicked() }
        binding.btnStop.setOnClickListener { viewModel.onStopClicked() }

        binding.cardTask.setOnClickListener {
            // Công việc gắn với phiên đang chạy nên chỉ đổi được khi timer đang IDLE.
            // Quy tắc nằm ở ViewModel — Fragment không tự đọc state của timer.
            if (viewModel.canSelectTask()) {
                openTaskSelector()
            }
        }
    }

    /**
     * Chưa chọn công việc thì mở Task Selector thay vì bắt đầu — mọi phiên tập trung đều phải
     * gắn với một công việc có thật (bảng `pomodoro_sessions` có khoá ngoại NOT NULL tới `tasks`).
     */
    private fun handlePrimaryAction() {
        if (!viewModel.onPrimaryAction()) {
            openTaskSelector()
        }
    }

    private fun openTaskSelector() {
        PomodoroTaskSelectorBottomSheet
            .newInstance(viewModel.currentSelectedTaskId)
            .show(childFragmentManager, PomodoroTaskSelectorBottomSheet.TAG)
    }

    private fun setupTaskSelectorResult() {
        // Fragment Result API: sống sót qua configuration change (không dùng lambda callback).
        childFragmentManager.setFragmentResultListener(
            PomodoroTaskSelectorBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val taskId = bundle.getLong(
                PomodoroTaskSelectorBottomSheet.RESULT_TASK_ID,
                Constants.NO_TASK_ID
            )
            if (taskId >= 0L) {
                viewModel.onTaskSelected(taskId)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Khi quay lại từ background, StateFlow phát ngay giá trị mới nhất nên UI
                // hiển thị đúng trạng thái hiện tại mà không cần đồng hồ riêng.
                combine(viewModel.uiState, viewModel.selectedTask) { snapshot, task ->
                    snapshot to task
                }.collect { (snapshot, task) ->
                    render(snapshot, task)
                }
            }
        }
    }

    private fun render(snapshot: PomodoroSnapshot, task: Task?) {
        val sessionColor = sessionColor(snapshot.sessionType)

        // 1. Session type
        binding.tvSessionType.setText(sessionLabelRes(snapshot.sessionType))
        binding.tvSessionType.setTextColor(sessionColor)

        // 2. Countdown + circular progress (vòng tròn cạn dần theo thời gian còn lại).
        // Khi IDLE, formattedDisplay xem trước độ dài phiên FOCUS theo cấu hình hiện tại.
        binding.tvCountdown.text = snapshot.formattedDisplay
        binding.tvTimerState.setText(stateLabelRes(snapshot.state))
        binding.viewProgressRing.setRingColor(sessionColor)
        binding.viewProgressRing.setRemainingFraction(remainingFraction(snapshot))

        // 3. Cycle indicator + tổng kết
        binding.tvCycleLabel.text = getString(
            R.string.pomodoro_cycle_label,
            snapshot.currentCycle.coerceAtLeast(1),
            snapshot.totalCycles
        )
        binding.tvFocusSummary.text = getString(
            R.string.pomodoro_completed_focus_count,
            snapshot.completedFocusSessions
        )
        renderCycleDots(snapshot, sessionColor)

        // 4. Công việc đang chọn
        renderTaskCard(snapshot, task)

        // 5. Trạng thái & enable/disable các nút
        renderControls(snapshot)
    }

    /**
     * Vòng tròn hiển thị phần **còn lại** của phiên: đầy lúc bắt đầu và cạn dần về 0
     * khi countdown về 00:00. Khi chưa chạy phiên nào thì hiển thị đầy.
     */
    private fun remainingFraction(snapshot: PomodoroSnapshot): Float =
        if (snapshot.state == PomodoroTimerState.IDLE) 1f else 1f - snapshot.progressFraction

    /**
     * Cycle Indicator: số chấm được tô = số phiên FOCUS đã hoàn thành trong chu kỳ hiện tại
     * (`focusSessionsInCurrentSet`), lấy trực tiếp từ state machine — không đếm lại ở UI.
     */
    private fun renderCycleDots(snapshot: PomodoroSnapshot, sessionColor: Int) {
        val total = snapshot.totalCycles
        if (total != builtCycleCount) {
            buildCycleDots(total)
            builtCycleCount = total
        }

        val filledCount = snapshot.focusSessionsInCurrentSet.coerceIn(0, total)
        val filledTint = ColorStateList.valueOf(sessionColor)

        cycleDots.forEachIndexed { index, dot ->
            val filled = index < filledCount
            dot.setBackgroundResource(
                if (filled) R.drawable.bg_cycle_dot_filled else R.drawable.bg_cycle_dot_empty
            )
            // Chấm đã hoàn thành lấy màu theo loại phiên để khớp với vòng tiến độ.
            if (filled) dot.backgroundTintList = filledTint
        }
    }

    private fun renderTaskCard(snapshot: PomodoroSnapshot, task: Task?) {
        val selectable = viewModel.canSelectTask(snapshot)

        binding.tvTaskLabel.setText(
            if (selectable) R.string.pomodoro_task_label else R.string.pomodoro_task_label_active
        )

        if (task == null) {
            binding.tvTaskTitle.setText(R.string.pomodoro_task_none)
            binding.tvTaskMeta.setText(R.string.pomodoro_task_none_hint)
            binding.ivTaskIcon.isVisible = false
        } else {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskMeta.text = PomodoroTaskFormatter.dueDateLabel(requireContext(), task)
            binding.ivTaskIcon.isVisible = true
        }

        // Không thể đổi công việc giữa phiên -> ẩn mũi tên để thể hiện trạng thái "đang khoá".
        binding.ivTaskChevron.isVisible = selectable
        binding.cardTask.isClickable = selectable
        binding.cardTask.isFocusable = selectable
    }

    private fun buildCycleDots(count: Int) {
        binding.cycleDotsContainer.removeAllViews()
        cycleDots.clear()

        val sizePx = dpToPx(DOT_SIZE_DP)
        val marginPx = dpToPx(DOT_MARGIN_DP)

        repeat(count) { index ->
            val dot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginStart = if (index == 0) 0 else marginPx
                }
                setBackgroundResource(R.drawable.bg_cycle_dot_empty)
            }
            binding.cycleDotsContainer.addView(dot)
            cycleDots += dot
        }
    }

    private fun renderControls(snapshot: PomodoroSnapshot) {
        val state = snapshot.state

        binding.btnPrimary.setText(
            when (state) {
                PomodoroTimerState.IDLE -> R.string.pomodoro_action_start
                PomodoroTimerState.PAUSED -> R.string.pomodoro_action_resume
                PomodoroTimerState.COMPLETED -> R.string.pomodoro_action_start_next
                PomodoroTimerState.RUNNING -> R.string.pomodoro_status_running
            }
        )
        // Nút chính: chỉ có ý nghĩa khi chưa chạy, đang tạm dừng, hoặc vừa xong phiên.
        binding.btnPrimary.setEnabledState(state != PomodoroTimerState.RUNNING)

        // Chỉ tạm dừng được khi đang chạy. Khi đang tạm dừng thì nút chính đảm nhiệm việc
        // tiếp tục, nên nút Pause bị disable (tránh 2 nút cùng chức năng).
        binding.btnPause.setText(
            if (state == PomodoroTimerState.PAUSED) {
                R.string.pomodoro_action_resume
            } else {
                R.string.pomodoro_action_pause
            }
        )
        binding.btnPause.setEnabledState(state == PomodoroTimerState.RUNNING)

        // Bỏ qua chỉ có nghĩa khi đang có phiên (chạy hoặc tạm dừng).
        binding.btnSkip.setEnabledState(
            state == PomodoroTimerState.RUNNING || state == PomodoroTimerState.PAUSED
        )

        // Dừng luôn khả dụng khi có phiên đang tồn tại (kể cả vừa hoàn thành).
        binding.btnStop.setEnabledState(state != PomodoroTimerState.IDLE)
    }

    private fun MaterialButton.setEnabledState(enabled: Boolean) {
        isEnabled = enabled
        alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    private fun sessionLabelRes(sessionType: SessionType): Int = when (sessionType) {
        SessionType.FOCUS -> R.string.pomodoro_session_focus
        SessionType.SHORT_BREAK -> R.string.pomodoro_session_short_break
        SessionType.LONG_BREAK -> R.string.pomodoro_session_long_break
    }

    private fun sessionColor(sessionType: SessionType): Int = ContextCompat.getColor(
        requireContext(),
        when (sessionType) {
            SessionType.FOCUS -> R.color.primary
            SessionType.SHORT_BREAK -> R.color.success
            SessionType.LONG_BREAK -> R.color.warning
        }
    )

    private fun stateLabelRes(state: PomodoroTimerState): Int = when (state) {
        PomodoroTimerState.IDLE -> R.string.pomodoro_status_idle
        PomodoroTimerState.RUNNING -> R.string.pomodoro_status_running
        PomodoroTimerState.PAUSED -> R.string.pomodoro_status_paused
        PomodoroTimerState.COMPLETED -> R.string.pomodoro_status_completed
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()

    private companion object {
        const val DOT_SIZE_DP = 10
        const val DOT_MARGIN_DP = 8
        const val DISABLED_ALPHA = 0.45f
    }
}
