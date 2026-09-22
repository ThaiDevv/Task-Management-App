package com.team.taskmanagementapp.ui.pomodoro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.BottomSheetPomodoroTaskSelectorBinding
import kotlinx.coroutines.launch

/**
 * Bottom sheet chọn công việc cho phiên Pomodoro (Task 10).
 *
 * - Danh sách lấy **trực tiếp từ Room** qua `TaskRepository.getAllTasks()`: chỉ những công việc
 *   người dùng đã tạo, không có dữ liệu mẫu và không hard-code id.
 * - Kết quả trả về bằng **Fragment Result API** (`setFragmentResult`) theo đúng convention của
 *   project — không truyền lambda callback vì callback kiểu đó không sống sót qua
 *   configuration change.
 */
class PomodoroTaskSelectorBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPomodoroTaskSelectorBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var adapter: PomodoroTaskAdapter

    private val repository: TaskRepository by lazy {
        TaskRepository(AppDatabase.getInstance(requireContext().applicationContext).taskDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPomodoroTaskSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PomodoroTaskAdapter { task ->
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_TASK_ID to task.id.toLong())
            )
            dismiss()
        }

        binding.recyclerTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTasks.adapter = adapter
        adapter.setSelectedTaskId(selectedTaskIdFromArgs())

        observeTasks()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getAllTasks().collect { tasks ->
                    adapter.submitList(tasks)
                    binding.tvEmptyState.isVisible = tasks.isEmpty()
                    binding.tvTaskCount.text =
                        getString(R.string.pomodoro_task_count_format, tasks.size)
                }
            }
        }
    }

    private fun selectedTaskIdFromArgs(): Long? =
        arguments?.getLong(ARG_SELECTED_TASK_ID)?.takeIf { it >= 0L }

    companion object {
        const val TAG = "PomodoroTaskSelector"

        /** Request key dùng chung giữa bottom sheet (gửi) và PomodoroFragment (nhận). */
        const val REQUEST_KEY = "pomodoro_task_selector_result"

        /** Key chứa taskId (Long) trong bundle kết quả. */
        const val RESULT_TASK_ID = "result_task_id"

        private const val ARG_SELECTED_TASK_ID = "arg_selected_task_id"
        private const val NO_TASK_ID = -1L

        /** @param selectedTaskId task đang được chọn để highlight trong danh sách, null nếu chưa chọn */
        fun newInstance(selectedTaskId: Long?): PomodoroTaskSelectorBottomSheet =
            PomodoroTaskSelectorBottomSheet().apply {
                arguments = bundleOf(ARG_SELECTED_TASK_ID to (selectedTaskId ?: NO_TASK_ID))
            }
    }
}
