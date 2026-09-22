package com.team.taskmanagementapp.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.team.taskmanagementapp.R
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
        val effectiveStreak = maxOf(currentStreak, bestStreak)

        // Bind all 7 daily streak milestone badges
        bindBadge(binding.layoutBadge3, binding.tvMilestone3, binding.tvStatus3, effectiveStreak >= 3)
        bindBadge(binding.layoutBadge7, binding.tvMilestone7, binding.tvStatus7, effectiveStreak >= 7)
        bindBadge(binding.layoutBadge30, binding.tvMilestone30, binding.tvStatus30, effectiveStreak >= 30)
        bindBadge(binding.layoutBadge50, binding.tvMilestone50, binding.tvStatus50, effectiveStreak >= 50)
        bindBadge(binding.layoutBadge100, binding.tvMilestone100, binding.tvStatus100, effectiveStreak >= 100)
        bindBadge(binding.layoutBadge200, binding.tvMilestone200, binding.tvStatus200, effectiveStreak >= 200)
        bindBadge(binding.layoutBadge365, binding.tvMilestone365, binding.tvStatus365, effectiveStreak >= 365)

        binding.btnCloseStreak.setOnClickListener {
            dismiss()
        }

        binding.btnStreakGotIt.setOnClickListener {
            dismiss()
        }
    }

    private fun bindBadge(layout: View, titleView: TextView, statusView: TextView, isUnlocked: Boolean) {
        val dp = resources.displayMetrics.density
        if (isUnlocked) {
            statusView.text = getString(R.string.badge_unlocked)
            statusView.setTextColor(Color.parseColor("#10B981"))
            titleView.setTextColor(Color.parseColor("#0F172A"))

            layout.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#F0FDF4")) // Light green tint
                setStroke((1 * dp).toInt(), Color.parseColor("#86EFAC"))
                cornerRadius = 14 * dp
            }
        } else {
            statusView.text = getString(R.string.badge_locked)
            statusView.setTextColor(Color.parseColor("#94A3B8"))
            titleView.setTextColor(Color.parseColor("#334155"))

            layout.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#F8FAFC"))
                setStroke((1 * dp).toInt(), Color.parseColor("#E2E8F0"))
                cornerRadius = 14 * dp
            }
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
