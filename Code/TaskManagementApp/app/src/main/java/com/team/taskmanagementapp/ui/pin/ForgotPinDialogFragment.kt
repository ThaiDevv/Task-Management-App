package com.team.taskmanagementapp.ui.pin

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team.taskmanagementapp.R

/**
 * DialogFragment wrapping the "Forgot PIN" informational dialog.
 *
 * Using a DialogFragment instead of a bare AlertDialog.Builder ensures the dialog
 * survives configuration changes (rotation, font-size, locale) because
 * FragmentManager automatically saves and restores committed DialogFragments.
 *
 * This dialog is purely informational — no callback is needed.
 */
class ForgotPinDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pin_forgot_dialog_title)
            .setMessage(R.string.pin_forgot_dialog_message)
            .setPositiveButton(R.string.pin_forgot_dialog_ok, null)
            .create()
    }

    companion object {
        /** Tag used when showing via FragmentManager — also used to detect duplicates. */
        const val TAG = "ForgotPinDialog"
    }
}
