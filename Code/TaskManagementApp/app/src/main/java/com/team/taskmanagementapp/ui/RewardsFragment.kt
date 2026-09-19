package com.team.taskmanagementapp.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.streak.StreakInfo
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentRewardsBinding
import com.team.taskmanagementapp.viewmodel.TaskViewModel
import com.team.taskmanagementapp.viewmodel.TaskViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: TaskViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        val repository = TaskRepository(database.taskDao())
        TaskViewModelFactory(repository, requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.streakInfo,
                    viewModel.totalCompletedTasks
                ) { streakInfo, completedTasks -> streakInfo to completedTasks }
                    .collect { (streakInfo, completedTasks) ->
                        updateRewardsUI(streakInfo, completedTasks)
                    }
            }
        }
    }

    private fun updateRewardsUI(streakInfo: StreakInfo, completedTasks: Int) {
        val streak = maxOf(streakInfo.currentStreak, streakInfo.bestStreak)

        val streakStatusViews = listOf(
            binding.tvStatus3,
            binding.tvStatus7,
            binding.tvStatus30,
            binding.tvStatus50,
            binding.tvStatus100,
            binding.tvStatus200,
            binding.tvStatus365
        )
        val taskStatusViews = listOf(
            binding.tvTaskStatus10,
            binding.tvTaskStatus50,
            binding.tvTaskStatus100,
            binding.tvTaskStatus250,
            binding.tvTaskStatus500,
            binding.tvTaskStatus1000,
            binding.tvTaskStatus2000
        )

        var unlockedCount = 0
        streakStatusViews.forEachIndexed { index, statusView ->
            if (applyBadgeStatus(statusView, streak >= STREAK_MILESTONES[index])) unlockedCount++
        }
        taskStatusViews.forEachIndexed { index, statusView ->
            if (applyBadgeStatus(statusView, completedTasks >= TASK_MILESTONES[index])) unlockedCount++
        }

        binding.tvUnlockedCount.text = getString(
            R.string.rewards_badges_unlocked_format,
            unlockedCount,
            STREAK_MILESTONES.size + TASK_MILESTONES.size
        )
        binding.tvTaskProgress.text = getString(R.string.task_milestones_progress, completedTasks)

        // Active streak (theo streak hiện tại, không lấy max như badge)
        binding.tvActiveStreakDays.text = when (streakInfo.currentStreak) {
            0 -> getString(R.string.streak_zero_days)
            1 -> getString(R.string.streak_one_day)
            else -> getString(R.string.streak_days_format, streakInfo.currentStreak)
        }
        binding.tvActiveStreakBest.text =
            getString(R.string.streak_best_format, streakInfo.bestStreak)
        binding.tvActiveStreakToday.text = getString(
            R.string.streak_today_progress,
            streakInfo.todayCompletedTasks,
            streakInfo.todayTotalTasks
        )

        binding.tvCurrentRank.text = resolveCurrentTitle(streakInfo, streak, completedTasks)
    }

    /**
     * Cập nhật text + màu cho một badge theo trạng thái mở/khóa.
     * @return true nếu badge đã mở.
     */
    private fun applyBadgeStatus(statusView: TextView, unlocked: Boolean): Boolean {
        statusView.text = getString(if (unlocked) R.string.badge_unlocked else R.string.badge_locked)
        statusView.setTextColor(Color.parseColor(if (unlocked) COLOR_UNLOCKED else COLOR_LOCKED))
        return unlocked
    }

    /**
     * Danh hiệu ở thẻ CURRENT TITLE = mốc cao nhất đạt được giữa streak và số task hoàn thành.
     * So sánh theo thứ hạng (tier) để hai thang đo khác nhau vẫn so sánh được; hòa thì ưu tiên streak.
     */
    private fun resolveCurrentTitle(streakInfo: StreakInfo, streak: Int, completedTasks: Int): String {
        val streakTier = STREAK_MILESTONES.indexOfLast { streak >= it }
        val taskTier = TASK_MILESTONES.indexOfLast { completedTasks >= it }

        return if (taskTier > streakTier) {
            getString(taskTitleRes(TASK_MILESTONES[taskTier]))
        } else {
            streakInfo.getBadgeTitle(streak)
        }
    }

    private fun taskTitleRes(milestone: Int): Int = when (milestone) {
        TASK_MILESTONES[0] -> R.string.task_title_novice
        TASK_MILESTONES[1] -> R.string.task_title_doer
        TASK_MILESTONES[2] -> R.string.task_title_achiever
        TASK_MILESTONES[3] -> R.string.task_title_executor
        TASK_MILESTONES[4] -> R.string.task_title_expert
        TASK_MILESTONES[5] -> R.string.task_title_champion
        else -> R.string.task_title_master
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        /** Thứ tự phải khớp với [streakStatusViews] trong updateRewardsUI. */
        val STREAK_MILESTONES = intArrayOf(3, 7, 30, 50, 100, 200, 365)

        /** Thứ tự phải khớp với [taskStatusViews] trong updateRewardsUI. */
        val TASK_MILESTONES = intArrayOf(10, 50, 100, 250, 500, 1000, 2000)

        const val COLOR_UNLOCKED = "#10B981"
        const val COLOR_LOCKED = "#737786"
    }
}
