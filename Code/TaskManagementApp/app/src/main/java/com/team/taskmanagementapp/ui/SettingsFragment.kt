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
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.databinding.FragmentSettingsBinding
import com.team.taskmanagementapp.ui.base.BaseActivity
import com.team.taskmanagementapp.ui.pin.PinLockActivity
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.NotificationPermissionManager
import com.team.taskmanagementapp.util.PinManager

/** Displays app settings and persists user-controlled toggle states. */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val preferences by lazy {
        requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private lateinit var pinManager: PinManager

    private var isSynchronizingSwitches = false

    // Enabling is completed inside PinLockActivity after the user sets and confirms a PIN.
    private val enablePinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            BaseActivity.isAppUnlockedInSession = true
            Toast.makeText(requireContext(), R.string.pin_enabled, Toast.LENGTH_SHORT).show()
        }
        // Also restores the switch when the user cancels PIN setup.
        synchronizeToggleStates()
    }

    // Disabling is intentionally owned by Settings: PinLockActivity only verifies the PIN.
    private val disablePinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pinManager.removePin()
            BaseActivity.isAppUnlockedInSession = false
            Toast.makeText(requireContext(), R.string.pin_disabled, Toast.LENGTH_SHORT).show()
        }
        // Reflect the persisted repository state for both success and cancellation.
        synchronizeToggleStates()
    }

    private val changePinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            BaseActivity.isAppUnlockedInSession = true
            showSnackbar(R.string.pin_change_success)
        }
        synchronizeToggleStates()
    }

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
        pinManager = PinManager.getInstance(requireContext())
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

            if (isChecked) {
                // Enable PIN - launch PinLockActivity in SET mode
                val intent = PinLockActivity.createIntent(requireContext(), PinLockActivity.PinMode.SET)
                enablePinLauncher.launch(intent)
            } else {
                // Disable PIN - launch in VERIFY mode to confirm current PIN first
                val intent = PinLockActivity.createIntent(requireContext(), PinLockActivity.PinMode.VERIFY_DISABLE)
                disablePinLauncher.launch(intent)
            }
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
            if (!pinManager.isPinEnabled()) return@setOnClickListener
            // Launch CHANGE mode - requires verifying old PIN first
            val intent = PinLockActivity.createIntent(requireContext(), PinLockActivity.PinMode.CHANGE)
            changePinLauncher.launch(intent)
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

        // Read PIN enabled state from EncryptedSharedPreferences via PinManager.
        val pinEnabled = pinManager.isPinEnabled()
        binding.pinLockSwitch.isChecked = pinEnabled
        binding.changePinDivider.isVisible = pinEnabled
        binding.changePinRow.isVisible = pinEnabled

        val notificationsEnabled = preferences.getBoolean(
            Constants.KEY_NOTIFICATIONS_ENABLED,
            true
        )
        binding.notificationSwitch.isChecked = notificationsEnabled &&
            NotificationPermissionManager.isGranted(requireContext())

        isSynchronizingSwitches = false
    }

    private fun showSnackbar(@StringRes messageRes: Int) {
        val root = _binding?.root ?: return
        Snackbar.make(root, messageRes, Snackbar.LENGTH_SHORT).show()
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
