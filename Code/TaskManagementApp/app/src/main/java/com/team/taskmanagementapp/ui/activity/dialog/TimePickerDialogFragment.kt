package com.team.taskmanagementapp.ui.activity.dialog

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import java.util.Calendar

/**
 * DialogFragment bọc TimePickerDialog để Android tự động restore dialog
 * khi configuration change (xoay màn hình, font size, locale, ...).
 */
class TimePickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val now = Calendar.getInstance()
        val hour = arguments?.getInt(ARG_HOUR) ?: now.get(Calendar.HOUR_OF_DAY)
        val minute = arguments?.getInt(ARG_MINUTE) ?: now.get(Calendar.MINUTE)

        return TimePickerDialog(
            requireContext(),
            { _, h, min ->
                setFragmentResult(REQUEST_KEY, bundleOf(ARG_HOUR to h, ARG_MINUTE to min))
            },
            hour,
            minute,
            false
        )
    }

    companion object {
        const val REQUEST_KEY = "time_picker_result"
        const val ARG_HOUR = "arg_hour"
        const val ARG_MINUTE = "arg_minute"

        fun newInstance(timeInMillis: Long): TimePickerDialogFragment {
            val cal = Calendar.getInstance().apply { timeInMillis = timeInMillis }
            return TimePickerDialogFragment().apply {
                arguments = bundleOf(
                    ARG_HOUR to cal.get(Calendar.HOUR_OF_DAY),
                    ARG_MINUTE to cal.get(Calendar.MINUTE)
                )
            }
        }
    }
}
