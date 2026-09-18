package com.team.taskmanagementapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.streak.StreakInfo
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentStreakBinding
import com.team.taskmanagementapp.viewmodel.TaskViewModel
import com.team.taskmanagementapp.viewmodel.TaskViewModelFactory
import kotlinx.coroutines.launch

class StreakFragment : Fragment() {

    private var _binding: FragmentStreakBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: TaskViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        val repository = TaskRepository(database.taskDao())
        TaskViewModelFactory(repository, requireContext().applicationContext)
    }

    private lateinit var streakWeekAdapter: StreakWeekAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStreakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        streakWeekAdapter = StreakWeekAdapter()
        binding.streakWeekRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = streakWeekAdapter
        }

        binding.cardStreakReward.setOnClickListener {
            openStreakDetails()
        }

        binding.btnViewStreakDetails.setOnClickListener {
            openStreakDetails()
        }
    }

    private fun openStreakDetails() {
        val currentStreak = viewModel.streakInfo.value
        StreakDetailsBottomSheet.newInstance(currentStreak)
            .show(childFragmentManager, StreakDetailsBottomSheet.TAG)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.streakInfo.collect { streakInfo ->
                    updateStreakUI(streakInfo)
                }
            }
        }
    }

    private fun updateStreakUI(streakInfo: StreakInfo) {
        binding.tvStreakTitle.text = when (streakInfo.currentStreak) {
            0 -> getString(R.string.streak_zero_days)
            1 -> getString(R.string.streak_one_day)
            else -> getString(R.string.streak_days_format, streakInfo.currentStreak)
        }

        binding.tvStreakSubtitle.text = when {
            streakInfo.isTodayCompleted -> getString(R.string.streak_subtitle_completed)
            streakInfo.currentStreak > 0 -> getString(R.string.streak_subtitle_active)
            else -> getString(R.string.streak_subtitle_zero)
        }

        binding.tvStreakBestBadge.text = getString(R.string.streak_best_format, streakInfo.bestStreak)
        binding.tvCurrentStreakStat.text = streakInfo.currentStreak.toString()
        binding.tvBestStreakStat.text = streakInfo.bestStreak.toString()
        binding.tvBadgeTitle.text = streakInfo.getBadgeTitle()

        streakWeekAdapter.submitList(streakInfo.weekDays)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
