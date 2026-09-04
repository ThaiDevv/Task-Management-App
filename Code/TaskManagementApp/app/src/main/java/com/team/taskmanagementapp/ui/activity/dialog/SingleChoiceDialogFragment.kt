package com.team.taskmanagementapp.ui.activity.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment

/**
 * DialogFragment danh sách chọn 1 mục (dùng cho Status và Reminder).
 * requestKey được truyền vào để phân biệt loại kết quả trả về.
 */
class SingleChoiceDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(ARG_TITLE).orEmpty()
        val options = arguments?.getStringArray(ARG_OPTIONS) ?: emptyArray()
        val requestKey = arguments?.getString(ARG_REQUEST_KEY).orEmpty()

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(options) { _, which ->
                setFragmentResult(requestKey, bundleOf(ARG_SELECTED_INDEX to which))
            }
            .create()
    }

    companion object {
        const val ARG_TITLE = "arg_title"
        const val ARG_OPTIONS = "arg_options"
        const val ARG_REQUEST_KEY = "arg_request_key"
        const val ARG_SELECTED_INDEX = "arg_selected_index"

        fun newInstance(
            title: String,
            options: Array<String>,
            requestKey: String
        ): SingleChoiceDialogFragment = SingleChoiceDialogFragment().apply {
            arguments = bundleOf(
                ARG_TITLE to title,
                ARG_OPTIONS to options,
                ARG_REQUEST_KEY to requestKey
            )
        }
    }
}
