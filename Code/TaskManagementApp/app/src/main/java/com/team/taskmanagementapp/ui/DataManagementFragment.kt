package com.team.taskmanagementapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.databinding.FragmentDataManagementBinding

/** Entry screen for backup and restore features implemented by later tasks. */
class DataManagementFragment : Fragment() {

    private var _binding: FragmentDataManagementBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        val showPendingMessage = View.OnClickListener {
            Snackbar.make(
                binding.root,
                R.string.data_feature_pending,
                Snackbar.LENGTH_SHORT
            ).show()
        }
        binding.exportDataButton.setOnClickListener(showPendingMessage)
        binding.restoreDataButton.setOnClickListener(showPendingMessage)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
