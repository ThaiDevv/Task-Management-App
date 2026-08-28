package com.team.taskmanagementapp.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.team.taskmanagementapp.MainActivity
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.databinding.ActivityPinLockBinding
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.PinManager

/**
 * PIN Lock Activity supporting multiple modes:
 * - ENTER: Verify PIN when opening app
 * - SET: Set new PIN when enabling PIN lock
 * - CHANGE: Change existing PIN (3 steps: verify old, enter new, confirm new)
 * - VERIFY_DISABLE: Verify PIN before disabling (used from Settings toggle OFF)
 */
class PinLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinLockBinding
    private lateinit var pinManager: PinManager

    private var mode: PinMode = PinMode.ENTER
    private var currentPin = StringBuilder()
    private var confirmPin: String? = null
    private var oldPin: String? = null

    private var failedAttempts = 0
    private var isLocked = false
    private var lockoutTimer: CountDownTimer? = null

    private val pinLength = 4

    enum class PinMode {
        ENTER,          // Verify PIN when opening app
        SET,            // Set new PIN when enabling
        CHANGE,         // Change existing PIN (3 steps)
        VERIFY_DISABLE  // Verify PIN before disabling (from Settings toggle OFF)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pinManager = PinManager.getInstance(this)
        mode = PinMode.valueOf(
            intent.getStringExtra(Constants.EXTRA_PIN_MODE) ?: PinMode.ENTER.name
        )

        setupUI()
        setupKeypad()
    }

    private fun setupUI() {
        when (mode) {
            PinMode.ENTER -> {
                binding.titleText.text = getString(R.string.pin_enter_title)
                binding.subtitleText.text = getString(R.string.pin_enter_subtitle)
            }
            PinMode.SET -> {
                binding.titleText.text = getString(R.string.pin_set_title)
                binding.subtitleText.text = getString(R.string.pin_set_subtitle)
            }
            PinMode.CHANGE -> {
                binding.titleText.text = getString(R.string.pin_change_title)
                binding.subtitleText.text = getString(R.string.pin_change_step1)
            }
            PinMode.VERIFY_DISABLE -> {
                binding.titleText.text = getString(R.string.pin_disable_title)
                binding.subtitleText.text = getString(R.string.pin_disable_subtitle)
            }
        }

        binding.pinDotsContainer.visibility = View.VISIBLE
        binding.lockoutContainer.visibility = View.GONE
        binding.errorText.visibility = View.GONE

        updatePinDots()
    }

    private fun setupKeypad() {
        // Number buttons 0-9
        binding.btn0.setOnClickListener { enterDigit('0') }
        binding.btn1.setOnClickListener { enterDigit('1') }
        binding.btn2.setOnClickListener { enterDigit('2') }
        binding.btn3.setOnClickListener { enterDigit('3') }
        binding.btn4.setOnClickListener { enterDigit('4') }
        binding.btn5.setOnClickListener { enterDigit('5') }
        binding.btn6.setOnClickListener { enterDigit('6') }
        binding.btn7.setOnClickListener { enterDigit('7') }
        binding.btn8.setOnClickListener { enterDigit('8') }
        binding.btn9.setOnClickListener { enterDigit('9') }

        // Delete button
        binding.btnDelete.setOnClickListener { deleteDigit() }
        binding.btnDelete.setOnLongClickListener {
            clearPin()
            true
        }

        // Biometric button (for future use)
        binding.btnBiometric.visibility = View.GONE

        // Forgot PIN - only in ENTER mode
        binding.forgotPinButton.visibility = if (mode == PinMode.ENTER) View.VISIBLE else View.GONE
        binding.forgotPinButton.setOnClickListener {
            Toast.makeText(this, R.string.pin_forgot_hint, Toast.LENGTH_SHORT).show()
        }
    }

    private fun enterDigit(digit: Char) {
        if (isLocked || currentPin.length >= pinLength) return

        currentPin.append(digit)
        updatePinDots()
        binding.errorText.visibility = View.GONE

        if (currentPin.length == pinLength) {
            binding.root.postDelayed({
                processPin()
            }, 200)
        }
    }

    private fun deleteDigit() {
        if (currentPin.isNotEmpty()) {
            currentPin.deleteCharAt(currentPin.length - 1)
            updatePinDots()
            binding.errorText.visibility = View.GONE
        }
    }

    private fun clearPin() {
        currentPin.clear()
        updatePinDots()
        binding.errorText.visibility = View.GONE
    }

    private fun updatePinDots() {
        val dots = listOf(
            binding.pinDot1,
            binding.pinDot2,
            binding.pinDot3,
            binding.pinDot4
        )

        dots.forEachIndexed { index, dot ->
            if (index < currentPin.length) {
                dot.isSelected = true
            } else {
                dot.isSelected = false
            }
        }
    }

    private fun processPin() {
        val pin = currentPin.toString()

        when (mode) {
            PinMode.ENTER -> handleEnterMode(pin)
            PinMode.SET -> handleSetMode(pin)
            PinMode.CHANGE -> handleChangeMode(pin)
            PinMode.VERIFY_DISABLE -> handleVerifyDisableMode(pin)
        }
    }

    private fun handleEnterMode(pin: String) {
        if (pinManager.verifyPin(pin)) {
            onPinSuccess()
        } else {
            failedAttempts++
            showError(getString(R.string.pin_error_wrong))

            if (failedAttempts >= Constants.MAX_PIN_ATTEMPTS) {
                startLockout()
            } else {
                shakeAndClear()
            }
        }
    }

    private fun handleSetMode(pin: String) {
        if (confirmPin == null) {
            // First entry - save and ask for confirmation
            confirmPin = pin
            binding.subtitleText.text = getString(R.string.pin_set_confirm_subtitle)
            clearPin()
            showSuccessAnimation()
        } else {
            // Confirmation entry
            if (pin == confirmPin) {
                if (pinManager.setPin(pin)) {
                    onPinSuccess()
                } else {
                    showError(getString(R.string.pin_error_set_failed))
                    confirmPin = null
                    binding.subtitleText.text = getString(R.string.pin_set_subtitle)
                    clearPin()
                }
            } else {
                showError(getString(R.string.pin_error_mismatch))
                confirmPin = null
                binding.subtitleText.text = getString(R.string.pin_set_subtitle)
                shakeAndClear()
            }
        }
    }

    private fun handleChangeMode(pin: String) {
        when {
            oldPin == null -> {
                // Step 1: Verify old PIN
                if (pinManager.verifyPin(pin)) {
                    oldPin = pin
                    binding.subtitleText.text = getString(R.string.pin_change_step2)
                    clearPin()
                    showSuccessAnimation()
                } else {
                    showError(getString(R.string.pin_error_wrong))
                    shakeAndClear()
                }
            }
            confirmPin == null -> {
                // Step 2: Enter new PIN
                confirmPin = pin
                binding.subtitleText.text = getString(R.string.pin_change_step3)
                clearPin()
                showSuccessAnimation()
            }
            else -> {
                // Step 3: Confirm new PIN
                if (pin == confirmPin) {
                    if (pinManager.setPin(pin)) {
                        Toast.makeText(this, R.string.pin_change_success, Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        showError(getString(R.string.pin_error_set_failed))
                        confirmPin = null
                        binding.subtitleText.text = getString(R.string.pin_change_step2)
                        clearPin()
                    }
                } else {
                    showError(getString(R.string.pin_error_mismatch))
                    confirmPin = null
                    binding.subtitleText.text = getString(R.string.pin_change_step2)
                    shakeAndClear()
                }
            }
        }
    }

    private fun handleVerifyDisableMode(pin: String) {
        if (pinManager.verifyPin(pin)) {
            // Correct PIN - disable PIN lock
            pinManager.removePin()
            Toast.makeText(this, R.string.pin_disabled, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            failedAttempts++
            showError(getString(R.string.pin_error_wrong))

            if (failedAttempts >= Constants.MAX_PIN_ATTEMPTS) {
                startLockout()
            } else {
                shakeAndClear()
            }
        }
    }

    private fun startLockout() {
        isLocked = true
        binding.pinDotsContainer.visibility = View.GONE
        binding.lockoutContainer.visibility = View.VISIBLE

        lockoutTimer = object : CountDownTimer(Constants.LOCKOUT_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.lockoutTimerText.text = getString(R.string.pin_lockout_countdown, seconds)
            }

            override fun onFinish() {
                endLockout()
            }
        }.start()
    }

    private fun endLockout() {
        isLocked = false
        failedAttempts = 0
        binding.pinDotsContainer.visibility = View.VISIBLE
        binding.lockoutContainer.visibility = View.GONE
        clearPin()
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
    }

    private fun shakeAndClear() {
        val animator = ObjectAnimator.ofFloat(binding.pinDotsContainer, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = 400
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()

        binding.root.postDelayed({
            clearPin()
        }, 400)
    }

    private fun showSuccessAnimation() {
        val dots = listOf(binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4)
        dots.forEachIndexed { index, dot ->
            binding.root.postDelayed({
                dot.setBackgroundResource(R.drawable.bg_pin_dot_filled)
            }, index * 50L)
        }
    }

    private fun onPinSuccess() {
        // Set RESULT_OK FIRST before any async operations
        setResult(RESULT_OK)

        // Success animation
        showSuccessAnimation()

        binding.root.postDelayed({
            when (mode) {
                PinMode.ENTER -> {
                    // Mark as verified in SharedPreferences for auto-lock tracking
                    getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_PIN_VERIFIED, true)
                        .putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
                        .apply()

                    // Start MainActivity and clear back stack
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                PinMode.SET -> {
                    // PIN was set successfully - just finish
                }
                PinMode.CHANGE -> {
                    // Already handled above with setResult + finish
                }
                PinMode.VERIFY_DISABLE -> {
                    // Already handled above
                }
            }
            finish()
        }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        lockoutTimer?.cancel()
    }

    companion object {
        const val KEY_LAST_ACTIVE_TIME = "key_last_active_time"
        const val KEY_PIN_VERIFIED = "key_pin_verified"

        fun createIntent(context: Context, mode: PinMode): Intent {
            return Intent(context, PinLockActivity::class.java).apply {
                putExtra(Constants.EXTRA_PIN_MODE, mode.name)
            }
        }
    }
}
