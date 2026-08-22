package com.team.taskmanagementapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.databinding.FragmentSettingsBinding
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.NotificationPermissionManager

/** Displays app settings and persists user-controlled toggle states. */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val preferences by lazy {
        requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private var isSynchronizingSwitches = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        preferences.edit()
            .putBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, isGranted)
            .apply()
        synchronizeToggleStates()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showAppVersion()
        setupToggleListeners()
        setupActions()
    }

    override fun onResume() {
        super.onResume()
        synchronizeToggleStates()
    }

    private fun setupToggleListeners() {
        binding.pinLockSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isSynchronizingSwitches) return@setOnCheckedChangeListener
            preferences.edit()
                .putBoolean(Constants.KEY_PIN_ENABLED, isChecked)
                .apply()
        }

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isSynchronizingSwitches) return@setOnCheckedChangeListener

            if (!isChecked) {
                preferences.edit()
                    .putBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, false)
                    .apply()
                return@setOnCheckedChangeListener
            }

            if (NotificationPermissionManager.isGranted(requireContext())) {
                preferences.edit()
                    .putBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, true)
                    .apply()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupActions() {
        binding.changePinRow.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.settings_pin_setup_pending,
                Toast.LENGTH_SHORT
            ).show()
        }

        val openDataManagement = View.OnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_dataManagementFragment
            )
        }
        binding.backupRow.setOnClickListener(openDataManagement)
        binding.restoreRow.setOnClickListener(openDataManagement)

        binding.systemPermissionsRow.setOnClickListener {
            openSystemNotificationSettings()
        }
    }

    private fun synchronizeToggleStates() {
        if (_binding == null) return

        isSynchronizingSwitches = true
        binding.pinLockSwitch.isChecked = preferences.getBoolean(
            Constants.KEY_PIN_ENABLED,
            false
        )

        val notificationsEnabled = preferences.getBoolean(
            Constants.KEY_NOTIFICATIONS_ENABLED,
            true
        )
        binding.notificationSwitch.isChecked = notificationsEnabled &&
            NotificationPermissionManager.isGranted(requireContext())
        isSynchronizingSwitches = false
    }

    @Suppress("DEPRECATION")
    private fun showAppVersion() {
        val versionName = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0)
            .versionName
            .orEmpty()
        binding.appVersionText.text = getString(
            R.string.settings_version_format,
            versionName
        )
    }

    private fun openSystemNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
