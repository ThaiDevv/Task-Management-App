package com.team.taskmanagementapp.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.team.taskmanagementapp.data.model.streak.StreakInfo
import com.team.taskmanagementapp.databinding.DialogStreakDetailsBottomSheetBinding

class StreakDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogStreakDetailsBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogStreakDetailsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentStreak = arguments?.getInt(ARG_CURRENT_STREAK, 0) ?: 0
        val bestStreak = arguments?.getInt(ARG_BEST_STREAK, 0) ?: 0

        val streakInfo = StreakInfo(currentStreak = currentStreak, bestStreak = bestStreak)

        binding.tvStreakModalCount.text = when (currentStreak) {
            1 -> "1 Day Streak! 🔥"
            else -> "$currentStreak Days Streak! 🔥"
        }
        binding.tvStreakModalBadge.text = streakInfo.getBadgeTitle()
        binding.tvCurrentStreakValue.text = currentStreak.toString()
        binding.tvBestStreakValue.text = bestStreak.toString()

        // Highlight achieved milestones
        if (currentStreak >= 3) {
            binding.tvMilestone3.text = "✅ " + binding.tvMilestone3.text
            binding.tvMilestone3.setTextColor(Color.parseColor("#10B981"))
        }
        if (currentStreak >= 7) {
            binding.tvMilestone7.text = "✅ " + binding.tvMilestone7.text
            binding.tvMilestone7.setTextColor(Color.parseColor("#10B981"))
        }
        if (currentStreak >= 14) {
            binding.tvMilestone14.text = "✅ " + binding.tvMilestone14.text
            binding.tvMilestone14.setTextColor(Color.parseColor("#10B981"))
        }
        if (currentStreak >= 30) {
            binding.tvMilestone30.text = "✅ " + binding.tvMilestone30.text
            binding.tvMilestone30.setTextColor(Color.parseColor("#10B981"))
        }

        binding.btnCloseStreak.setOnClickListener {
            dismiss()
        }

        binding.btnStreakGotIt.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "StreakDetailsBottomSheet"
        private const val ARG_CURRENT_STREAK = "arg_current_streak"
        private const val ARG_BEST_STREAK = "arg_best_streak"

        fun newInstance(streakInfo: StreakInfo): StreakDetailsBottomSheet {
            return StreakDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_CURRENT_STREAK, streakInfo.currentStreak)
                    putInt(ARG_BEST_STREAK, streakInfo.bestStreak)
                }
            }
        }
    }
}
