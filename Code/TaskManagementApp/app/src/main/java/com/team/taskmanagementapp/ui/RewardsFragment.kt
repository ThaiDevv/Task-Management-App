package com.team.taskmanagementapp.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.streak.StreakInfo
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentRewardsBinding
import com.team.taskmanagementapp.viewmodel.TaskViewModel
import com.team.taskmanagementapp.viewmodel.TaskViewModelFactory
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
                viewModel.streakInfo.collect { streakInfo ->
                    updateRewardsUI(streakInfo)
                }
            }
        }
    }

    private fun updateRewardsUI(streakInfo: StreakInfo) {
        val streak = maxOf(streakInfo.currentStreak, streakInfo.bestStreak)
        var unlockedCount = 0

        if (streak >= 3) {
            unlockedCount++
            binding.tvStatus3.text = "Unlocked ✅"
            binding.tvStatus3.setTextColor(Color.parseColor("#10B981"))
        } else {
            binding.tvStatus3.text = "Locked 🔒"
            binding.tvStatus3.setTextColor(Color.parseColor("#737786"))
        }

        if (streak >= 7) {
            unlockedCount++
            binding.tvStatus7.text = "Unlocked ✅"
            binding.tvStatus7.setTextColor(Color.parseColor("#10B981"))
        } else {
            binding.tvStatus7.text = "Locked 🔒"
            binding.tvStatus7.setTextColor(Color.parseColor("#737786"))
        }

        if (streak >= 14) {
            unlockedCount++
            binding.tvStatus14.text = "Unlocked ✅"
            binding.tvStatus14.setTextColor(Color.parseColor("#10B981"))
        } else {
            binding.tvStatus14.text = "Locked 🔒"
            binding.tvStatus14.setTextColor(Color.parseColor("#737786"))
        }

        if (streak >= 30) {
            unlockedCount++
            binding.tvStatus30.text = "Unlocked ✅"
            binding.tvStatus30.setTextColor(Color.parseColor("#10B981"))
        } else {
            binding.tvStatus30.text = "Locked 🔒"
            binding.tvStatus30.setTextColor(Color.parseColor("#737786"))
        }

        if (streak >= 100) {
            unlockedCount++
            binding.tvStatus100.text = "Unlocked ✅"
            binding.tvStatus100.setTextColor(Color.parseColor("#10B981"))
        } else {
            binding.tvStatus100.text = "Locked 🔒"
            binding.tvStatus100.setTextColor(Color.parseColor("#737786"))
        }

        binding.tvUnlockedCount.text = "$unlockedCount / 5"
        binding.tvCurrentRank.text = streakInfo.getBadgeTitle()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
