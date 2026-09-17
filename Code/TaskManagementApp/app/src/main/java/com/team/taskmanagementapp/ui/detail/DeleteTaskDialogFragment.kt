package com.team.taskmanagementapp.ui.detail

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team.taskmanagementapp.R

class DeleteTaskDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isRecurring = arguments?.getBoolean(ARG_IS_RECURRING) ?: false

        return if (isRecurring) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.task_detail_delete_recurring_title)
                .setMessage(R.string.task_detail_delete_recurring_msg)
                .setNegativeButton(R.string.task_detail_delete_only_this) { _, _ ->
                    setFragmentResult(REQUEST_KEY, bundleOf(RESULT_DELETE_TYPE to DELETE_ONLY_THIS))
                }
                .setPositiveButton(R.string.task_detail_delete_all_occurrences) { _, _ ->
                    setFragmentResult(REQUEST_KEY, bundleOf(RESULT_DELETE_TYPE to DELETE_ALL))
                }
                .setNeutralButton(R.string.action_cancel, null)
                .create()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.task_detail_delete_confirm_title)
                .setMessage(R.string.task_detail_delete_confirm_msg)
                .setNegativeButton(R.string.task_detail_delete_confirm_negative, null)
                .setPositiveButton(R.string.task_detail_delete_confirm_positive) { _, _ ->
                    setFragmentResult(REQUEST_KEY, bundleOf(RESULT_DELETE_TYPE to DELETE_NORMAL))
                }
                .create()
        }
    }

    companion object {
        const val TAG = "DeleteTaskDialogFragment"
        const val REQUEST_KEY = "delete_task_request"
        const val RESULT_DELETE_TYPE = "result_delete_type"
        
        const val DELETE_NORMAL = 0
        const val DELETE_ONLY_THIS = 1
        const val DELETE_ALL = 2
        
        private const val ARG_IS_RECURRING = "arg_is_recurring"

        fun newInstance(isRecurring: Boolean): DeleteTaskDialogFragment {
            return DeleteTaskDialogFragment().apply {
                arguments = bundleOf(ARG_IS_RECURRING to isRecurring)
            }
        }
    }
}
