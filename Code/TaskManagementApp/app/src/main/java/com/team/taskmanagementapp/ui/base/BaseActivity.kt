package com.team.taskmanagementapp.ui.base

import android.content.Context
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.team.taskmanagementapp.ui.PinLockActivity
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.PinManager

/**
 * Base Activity that handles PIN lock flow and auto-lock functionality.
 * Uses ProcessLifecycleOwner + DefaultLifecycleObserver to detect app background/foreground transitions.
 *
 * Auto-lock logic:
 * - When app goes to background for > 1 minute, PIN will be required on resume
 * - PIN check happens on app start (if PIN is enabled)
 */
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var pinManager: PinManager
    private var pendingPinMode: PinLockActivity.PinMode? = null

    // Activity result launcher for PIN verification
    private val pinLockLauncher: ActivityResultLauncher<android.content.Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (pendingPinMode) {
                PinLockActivity.PinMode.ENTER -> {
                    if (result.resultCode == RESULT_OK) {
                        markPinVerified()
                    }
                }
                PinLockActivity.PinMode.SET,
                PinLockActivity.PinMode.CHANGE,
                PinLockActivity.PinMode.VERIFY_DISABLE -> {
                    if (result.resultCode == RESULT_OK) {
                        markPinVerified()
                    }
                }
                null -> { /* No pending PIN action */ }
            }
            pendingPinMode = null
        }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            // Check auto-lock when app comes to foreground
            checkAutoLock()
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            // SAVE timestamp to SharedPreferences when going to background
            saveBackgroundTimestamp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinManager = PinManager.getInstance(this)

        // Register lifecycle observer for auto-lock
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    override fun onResume() {
        super.onResume()
        // Check PIN on app start
        if (pendingPinMode == null) {
            checkPinRequired()
        }
    }

    /**
     * Check if PIN verification is required on app start.
     */
    private fun checkPinRequired() {
        if (!pinManager.isPinEnabled()) {
            return
        }

        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val isVerified = prefs.getBoolean(PinLockActivity.KEY_PIN_VERIFIED, false)

        if (!isVerified) {
            launchPinLock(PinLockActivity.PinMode.ENTER)
        }
    }

    /**
     * Save current timestamp when app goes to background.
     * This is used to calculate how long app was in background.
     */
    private fun saveBackgroundTimestamp() {
        getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(PinLockActivity.KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Check if auto-lock should trigger based on background duration.
     */
    private fun checkAutoLock() {
        if (!pinManager.isPinEnabled()) {
            clearPinVerification()
            return
        }

        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val lastActive = prefs.getLong(PinLockActivity.KEY_LAST_ACTIVE_TIME, 0L)
        val autoLockTimeout = pinManager.getAutoLockTimeout()

        if (lastActive == 0L) {
            // First time or PIN just enabled - require PIN
            clearPinVerification()
            return
        }

        val timeInBackground = System.currentTimeMillis() - lastActive
        if (timeInBackground > autoLockTimeout) {
            // Background > timeout (e.g., 1 minute) - require PIN again
            clearPinVerification()
        }
        // If timeInBackground <= timeout, PIN is still valid - don't ask again
    }

    /**
     * Call this after PIN is successfully verified.
     * Updates both verified flag and last active timestamp.
     */
    protected fun markPinVerified() {
        getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PinLockActivity.KEY_PIN_VERIFIED, true)
            .putLong(PinLockActivity.KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Call this to clear PIN verification (e.g., when PIN is disabled).
     */
    protected fun clearPinVerification() {
        getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PinLockActivity.KEY_PIN_VERIFIED, false)
            .apply()
    }

    /**
     * Launch PinLockActivity for the given mode.
     * @param mode ENTER (verify), SET (enable), or CHANGE (change)
     */
    protected fun launchPinLock(mode: PinLockActivity.PinMode) {
        pendingPinMode = mode
        val intent = PinLockActivity.createIntent(this, mode)
        pinLockLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
    }
}
