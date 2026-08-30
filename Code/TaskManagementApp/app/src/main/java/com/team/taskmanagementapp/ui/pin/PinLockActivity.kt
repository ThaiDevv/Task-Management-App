package com.team.taskmanagementapp.ui.pin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.databinding.ActivityPinLockBinding
import com.team.taskmanagementapp.databinding.ItemPinKeyWithLettersBinding
import com.team.taskmanagementapp.pinRepository
import com.team.taskmanagementapp.security.PinRepository
import com.team.taskmanagementapp.ui.base.BaseActivity
import com.team.taskmanagementapp.util.Constants

/**
 * PinLockActivity — Màn hình nhập và quản lý PIN (TASK-25, TMA-47, TMA-48 & TMA-49).
 *
 * Hỗ trợ 4 chế độ hoạt động:
 * - ENTER: Mở khóa ứng dụng (xác thực PIN khi khởi động hoặc sau auto-lock)
 * - SET: Thiết lập mã PIN mới (2 bước: nhập PIN mới -> xác nhận lại PIN)
 * - CHANGE: Đổi mã PIN (3 bước: xác thực PIN cũ -> nhập PIN mới -> xác nhận lại PIN)
 * - VERIFY_DISABLE: Xác thực PIN trước khi tắt tính năng khóa PIN trong Settings
 *
 * Tính năng UI:
 * - Hiển thị 4 chấm vector indicator sắc nét tương ứng với số ký tự đã nhập.
 * - Keypad 3×4 (nút tròn phẳng 76dp, 1–9 + letters + fingerprint/0/backspace) chuẩn thiết kế TaskFlow.
 * - Animation scale khi nhấn phím & Shake animation khi nhập sai PIN.
 * - Brute-force lockout: khoá 30s sau 5 lần sai liên tiếp + CountDownTimer.
 * - Hỗ trợ Forgot PIN dialog trong chế độ ENTER.
 */
class PinLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinLockBinding
    private lateinit var pinRepo: PinRepository

    private var mode: PinMode = PinMode.ENTER
    private val pinBuffer = StringBuilder()
    private val maxPinLength = 4

    private var confirmPin: String? = null
    private val changePinFlow = ChangePinFlow()

    private var lockoutTimer: CountDownTimer? = null

    enum class PinMode {
        ENTER,          // Verify PIN when opening app
        SET,            // Set new PIN when enabling PIN lock
        CHANGE,         // Change existing PIN (3 steps: old -> new -> confirm)
        VERIFY_DISABLE  // Verify PIN before disabling from Settings
    }

    // ──────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pinRepo = pinRepository()
        mode = try {
            PinMode.valueOf(intent.getStringExtra(Constants.EXTRA_PIN_MODE) ?: PinMode.ENTER.name)
        } catch (e: Exception) {
            PinMode.ENTER
        }

        supportActionBar?.hide()

        setupHeader()
        setupKeypad()
        setupActionButtons()
        checkLockoutOnResume()
    }

    override fun onResume() {
        super.onResume()
        checkLockoutOnResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        lockoutTimer?.cancel()
    }

    // ──────────────────────────────────────────────────────────────────────
    // UI Setup
    // ──────────────────────────────────────────────────────────────────────

    private fun setupHeader() {
        when (mode) {
            PinMode.ENTER -> {
                binding.tvWelcomeBack.text = getString(R.string.pin_enter_title)
                binding.tvSubtitle.text = getString(R.string.pin_enter_subtitle)
                binding.btnForgotPin.visibility = View.VISIBLE
            }
            PinMode.SET -> {
                binding.tvWelcomeBack.text = getString(R.string.pin_set_title)
                binding.tvSubtitle.text = getString(R.string.pin_set_subtitle)
                binding.btnForgotPin.visibility = View.GONE
            }
            PinMode.CHANGE -> {
                binding.tvWelcomeBack.text = getString(R.string.pin_change_title)
                binding.tvSubtitle.text = getString(R.string.pin_change_step1)
                binding.btnForgotPin.visibility = View.GONE
            }
            PinMode.VERIFY_DISABLE -> {
                binding.tvWelcomeBack.text = getString(R.string.pin_disable_title)
                binding.tvSubtitle.text = getString(R.string.pin_disable_subtitle)
                binding.btnForgotPin.visibility = View.GONE
            }
        }
        updatePinDots()
    }

    private fun setupKeypad() {
        val allKeys = listOf(
            Triple(binding.keyBtn1.root, 1, ""),
            Triple(binding.keyBtn2.root, 2, "ABC"),
            Triple(binding.keyBtn3.root, 3, "DEF"),
            Triple(binding.keyBtn4.root, 4, "GHI"),
            Triple(binding.keyBtn5.root, 5, "JKL"),
            Triple(binding.keyBtn6.root, 6, "MNO"),
            Triple(binding.keyBtn7.root, 7, "PQRS"),
            Triple(binding.keyBtn8.root, 8, "TUV"),
            Triple(binding.keyBtn9.root, 9, "WXYZ"),
            Triple(binding.keyBtn0.root, 0, "")
        )

        allKeys.forEach { (root, digit, letters) ->
            val keyBinding = ItemPinKeyWithLettersBinding.bind(root)
            keyBinding.tvKeyNumber.text = digit.toString()
            if (letters.isNotEmpty()) {
                keyBinding.tvKeyLetters.text = letters
                keyBinding.tvKeyLetters.visibility = View.VISIBLE
            } else {
                keyBinding.tvKeyLetters.visibility = View.GONE
            }
            keyBinding.pinKeyCard.setOnClickListener {
                animateKeyPress(it)
                onDigitPressed(digit)
            }
        }
    }

    private fun setupActionButtons() {
        // Backspace
        binding.btnBackspace.setOnClickListener {
            animateKeyPress(it)
            onBackspacePressed()
        }
        binding.btnBackspace.setOnLongClickListener {
            pinBuffer.clear()
            updatePinDots()
            hideError()
            true
        }

        // Biometric button (placeholder for future biometric feature)
        binding.btnFingerprint.setOnClickListener {
            animateKeyPress(it)
        }

        // Forgot PIN (only in ENTER mode)
        binding.btnForgotPin.setOnClickListener {
            showForgotPinDialog()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // PIN Processing Logic
    // ──────────────────────────────────────────────────────────────────────

    private fun onDigitPressed(digit: Int) {
        if (pinRepo.isLockedOut()) return
        if (pinBuffer.length >= maxPinLength) return

        pinBuffer.append(digit)
        updatePinDots()
        hideError()

        if (pinBuffer.length == maxPinLength) {
            binding.root.postDelayed({
                processPin()
            }, 150L)
        }
    }

    private fun onBackspacePressed() {
        if (pinBuffer.isNotEmpty()) {
            pinBuffer.deleteCharAt(pinBuffer.length - 1)
            updatePinDots()
            hideError()
        }
    }

    private fun processPin() {
        val pin = pinBuffer.toString()
        when (mode) {
            PinMode.ENTER -> handleEnterMode(pin)
            PinMode.SET -> handleSetMode(pin)
            PinMode.CHANGE -> handleChangeMode(pin)
            PinMode.VERIFY_DISABLE -> handleVerifyDisableMode(pin)
        }
    }

    private fun handleEnterMode(pin: String) {
        if (pinRepo.verifyPin(pin)) {
            onPinSuccess()
        } else {
            handleFailedPinAttempt()
        }
    }

    private fun handleSetMode(pin: String) {
        if (confirmPin == null) {
            confirmPin = pin
            binding.tvSubtitle.text = getString(R.string.pin_set_confirm_subtitle)
            clearBuffer()
        } else {
            if (pin == confirmPin) {
                pinRepo.setPin(pin)
                BaseActivity.isAppUnlockedInSession = true
                markPinVerified()
                setResult(RESULT_OK)
                finish()
            } else {
                showError(getString(R.string.pin_error_mismatch))
                confirmPin = null
                binding.tvSubtitle.text = getString(R.string.pin_set_subtitle)
                shakeAndClear()
            }
        }
    }

    private fun handleChangeMode(pin: String) {
        when (val submission = changePinFlow.submit(pin, pinRepo::verifyPin)) {
            is ChangePinFlow.Submission.CurrentPinRejected -> {
                handleFailedPinAttempt()
            }

            is ChangePinFlow.Submission.AwaitingNewPin -> {
                pinRepo.resetFailedAttempts()
                binding.tvSubtitle.text = getString(R.string.pin_change_step2)
                clearBuffer()
            }

            is ChangePinFlow.Submission.AwaitingConfirmation -> {
                binding.tvSubtitle.text = getString(R.string.pin_change_step3)
                clearBuffer()
            }

            is ChangePinFlow.Submission.NewPinMismatch -> {
                showError(getString(R.string.pin_error_mismatch))
                binding.tvSubtitle.text = getString(R.string.pin_change_step2)
                shakeAndClear()
            }

            is ChangePinFlow.Submission.Completed -> {
                pinRepo.setPin(submission.newPin)
                BaseActivity.isAppUnlockedInSession = true
                markPinVerified()
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun handleVerifyDisableMode(pin: String) {
        if (pinRepo.verifyPin(pin)) {
            // The caller owns the destructive action. This mode only verifies the current PIN.
            pinRepo.resetFailedAttempts()
            setResult(RESULT_OK)
            finish()
        } else {
            handleFailedPinAttempt()
        }
    }

    private fun handleFailedPinAttempt() {
        val remainingAttempts = Constants.MAX_PIN_ATTEMPTS - pinRepo.recordFailedAttempt()
        if (pinRepo.isLockedOut()) {
            startLockoutCountdown()
        } else {
            showError(getString(R.string.pin_error_wrong_vi, remainingAttempts))
        }
        shakeAndClear()
    }

    private fun onPinSuccess() {
        pinRepo.resetFailedAttempts()
        BaseActivity.isAppUnlockedInSession = true
        markPinVerified()
        setResult(RESULT_OK)
        finish()
    }

    private fun markPinVerified() {
        getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PIN_VERIFIED, true)
            .putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
            .apply()
    }

    private fun clearBuffer() {
        pinBuffer.clear()
        updatePinDots()
    }

    private fun shakeAndClear() {
        shakePinIndicator()
        binding.pinIndicatorContainer.postDelayed({
            clearBuffer()
        }, 400L)
    }

    // ──────────────────────────────────────────────────────────────────────
    // UI — PIN Dots & Animations
    // ──────────────────────────────────────────────────────────────────────

    private val pinDots by lazy {
        listOf(binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4)
    }

    private fun updatePinDots() {
        pinDots.forEachIndexed { index, dot ->
            val filled = index < pinBuffer.length
            dot.setImageResource(
                if (filled) R.drawable.ic_pin_dot_filled else R.drawable.ic_pin_dot_empty
            )
            dot.animate()
                .scaleX(if (filled) 1.15f else 1.0f)
                .scaleY(if (filled) 1.15f else 1.0f)
                .setDuration(120)
                .start()
        }
    }

    private fun animateKeyPress(view: View) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(80)
                    .start()
            }
            .start()
    }

    private fun shakePinIndicator() {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake_pin)
        binding.pinIndicatorContainer.startAnimation(shake)
    }

    private fun showError(message: String) {
        binding.tvErrorMessage.text = message
        binding.tvErrorMessage.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvErrorMessage.visibility = View.INVISIBLE
    }

    // ──────────────────────────────────────────────────────────────────────
    // Lockout Management
    // ──────────────────────────────────────────────────────────────────────

    private fun checkLockoutOnResume() {
        if (pinRepo.isLockedOut()) {
            startLockoutCountdown()
        }
    }

    private fun startLockoutCountdown() {
        val remaining = pinRepo.getLockoutEndTime() - System.currentTimeMillis()
        if (remaining <= 0) {
            pinRepo.resetFailedAttempts()
            setKeypadEnabled(true)
            hideError()
            return
        }

        setKeypadEnabled(false)
        clearBuffer()

        lockoutTimer?.cancel()
        lockoutTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt() + 1
                showError(getString(R.string.pin_error_locked_vi, seconds))
            }

            override fun onFinish() {
                pinRepo.resetFailedAttempts()
                setKeypadEnabled(true)
                hideError()
            }
        }.start()
    }

    private fun setKeypadEnabled(enabled: Boolean) {
        binding.btnBackspace.isEnabled = enabled
        binding.btnFingerprint.isEnabled = enabled

        listOf(
            binding.keyBtn1.pinKeyCard,
            binding.keyBtn2.pinKeyCard,
            binding.keyBtn3.pinKeyCard,
            binding.keyBtn4.pinKeyCard,
            binding.keyBtn5.pinKeyCard,
            binding.keyBtn6.pinKeyCard,
            binding.keyBtn7.pinKeyCard,
            binding.keyBtn8.pinKeyCard,
            binding.keyBtn9.pinKeyCard,
            binding.keyBtn0.pinKeyCard
        ).forEach { it.isEnabled = enabled }
    }

    private fun showForgotPinDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.pin_forgot_dialog_title)
            .setMessage(R.string.pin_forgot_dialog_message)
            .setPositiveButton(R.string.pin_forgot_dialog_ok, null)
            .show()
    }

    @Deprecated("Use OnBackPressedDispatcher", level = DeprecationLevel.WARNING)
    override fun onBackPressed() {
        if (mode != PinMode.ENTER) {
            super.onBackPressed()
        }
        // In ENTER mode, block back to prevent bypassing lock screen
    }

    // ──────────────────────────────────────────────────────────────────────
    // Companion Object
    // ──────────────────────────────────────────────────────────────────────

    companion object {
        const val KEY_PIN_VERIFIED = "key_pin_verified"
        const val KEY_LAST_ACTIVE_TIME = "key_last_active_time"

        fun createIntent(context: Context, mode: PinMode = PinMode.ENTER): Intent =
            Intent(context, PinLockActivity::class.java).apply {
                putExtra(Constants.EXTRA_PIN_MODE, mode.name)
            }

        fun start(context: Context, mode: PinMode = PinMode.ENTER) {
            context.startActivity(createIntent(context, mode))
        }
    }
}
