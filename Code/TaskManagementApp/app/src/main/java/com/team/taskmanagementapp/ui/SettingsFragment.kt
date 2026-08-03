package com.team.taskmanagementapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.team.taskmanagementapp.databinding.FragmentSettingsBinding

/**
 * SettingsFragment displays security settings including PIN lock,
 * biometric authentication, and auto-lock timer configuration.
 */
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        // Setup PIN Lock Toggle
        binding.pinLockSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.pinSettingsGroup.alpha = if (isChecked) 1f else 0.5f
            binding.pinSettingsGroup.isEnabled = isChecked
        }

        // Setup Auto-lock Spinner
        val autoLockOptions = arrayOf(
            "Immediately",
            "1 minute",
            "5 minutes",
            "15 minutes",
            "Never"
        )
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            autoLockOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.autoLockSpinner.adapter = adapter
        binding.autoLockSpinner.setSelection(2) // Default: 5 minutes

        // Setup Buttons
        binding.logoutButton.setOnClickListener {
            // Handle logout action
        }

        binding.deactivateButton.setOnClickListener {
            // Handle deactivate action
        }
    }
}
