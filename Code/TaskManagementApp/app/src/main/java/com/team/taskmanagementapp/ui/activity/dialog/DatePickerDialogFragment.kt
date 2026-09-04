package com.team.taskmanagementapp.ui.activity.dialog

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import java.util.Calendar

/**
 * DialogFragment bọc DatePickerDialog để Android tự động restore dialog
 * khi configuration change (xoay màn hình, font size, locale, ...).
 */
class DatePickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val now = Calendar.getInstance()
        val year = arguments?.getInt(ARG_YEAR) ?: now.get(Calendar.YEAR)
        val month = arguments?.getInt(ARG_MONTH) ?: now.get(Calendar.MONTH)
        val day = arguments?.getInt(ARG_DAY) ?: now.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(requireContext(), { _, y, m, d ->
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(ARG_YEAR to y, ARG_MONTH to m, ARG_DAY to d)
            )
        }, year, month, day)
    }

    companion object {
        const val REQUEST_KEY = "date_picker_result"
        const val ARG_YEAR = "arg_year"
        const val ARG_MONTH = "arg_month"
        const val ARG_DAY = "arg_day"

        fun newInstance(dateInMillis: Long): DatePickerDialogFragment {
            val cal = Calendar.getInstance().apply { timeInMillis = dateInMillis }
            return DatePickerDialogFragment().apply {
                arguments = bundleOf(
                    ARG_YEAR to cal.get(Calendar.YEAR),
                    ARG_MONTH to cal.get(Calendar.MONTH),
                    ARG_DAY to cal.get(Calendar.DAY_OF_MONTH)
                )
            }
        }
    }
}
