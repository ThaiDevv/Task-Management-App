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
import com.team.taskmanagementapp.security.PinRepository
import com.team.taskmanagementapp.pinRepository
import com.team.taskmanagementapp.util.Constants

/**
 * PinLockActivity — Màn hình nhập PIN để mở khóa ứng dụng (TASK-25).
 *
 * Tính năng:
 * - Hiển thị 4 chấm indicator tương ứng với số ký tự đã nhập.
 * - Keypad 3×4 (1–9 + fingerprint/0/backspace) khớp hoàn toàn với design.
 * - Animation scale-down khi nhấn phím.
 * - Shake animation khi nhập sai PIN.
 * - Brute-force lockout: khoá 30s sau 5 lần sai liên tiếp + CountDownTimer.
 * - "Forgot PIN?" dialog.
 *
 * Cách dùng:
 *   PinLockActivity.start(context)
 *   // hoặc với result
 *   PinLockActivity.createIntent(context)
 */
class PinLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinLockBinding
    private lateinit var pinRepo: PinRepository

    // PIN đang được nhập, tối đa 4 ký tự
    private val pinBuffer = StringBuilder()
    private val maxPinLength = 4

    // CountDownTimer cho lockout
    private var lockoutTimer: CountDownTimer? = null

    // ──────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pinRepo = pinRepository()

        // Ẩn ActionBar nếu có (màn hình toàn màn hình)
        supportActionBar?.hide()

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
    // Setup
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Gán số + letters cho tất cả keypad buttons (2–9 dùng include layout).
     * Gán click listener cho từng nút.
     */
    private fun setupKeypad() {
        // Key 1 — số đơn, không có letters
        binding.btn1.setOnClickListener { onDigitPressed(1) }

        // Keys 2–9 với letters
        val keysWithLetters = listOf(
            Triple(binding.keyBtn2.root, 2, "ABC"),
            Triple(binding.keyBtn3.root, 3, "DEF"),
            Triple(binding.keyBtn4.root, 4, "GHI"),
            Triple(binding.keyBtn5.root, 5, "JKL"),
            Triple(binding.keyBtn6.root, 6, "MNO"),
            Triple(binding.keyBtn7.root, 7, "PQRS"),
            Triple(binding.keyBtn8.root, 8, "TUV"),
            Triple(binding.keyBtn9.root, 9, "WXYZ")
        )

        keysWithLetters.forEach { (root, digit, letters) ->
            // root là FrameLayout — bind inner LinearLayout qua ItemPinKeyWithLettersBinding
            val keyBinding = ItemPinKeyWithLettersBinding.bind(root)
            keyBinding.tvKeyNumber.text = digit.toString()
            keyBinding.tvKeyLetters.text = letters
            keyBinding.pinKeyRoot.setOnClickListener {
                animateKeyPress(it)
                onDigitPressed(digit)
            }
        }

        // Key 0
        binding.btn0.setOnClickListener { onDigitPressed(0) }
    }

    private fun setupActionButtons() {
        // Backspace
        binding.btnBackspace.setOnClickListener {
            onBackspacePressed()
        }

        // Fingerprint (placeholder — TASK-47 sẽ implement BiometricPrompt)
        binding.btnFingerprint.setOnClickListener {
            // TODO: Implement biometric in TASK-47
        }

        // Forgot PIN
        binding.btnForgotPin.setOnClickListener {
            showForgotPinDialog()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // PIN Logic
    // ──────────────────────────────────────────────────────────────────────

    private fun onDigitPressed(digit: Int) {
        if (pinRepo.isLockedOut()) return
        if (pinBuffer.length >= maxPinLength) return

        pinBuffer.append(digit)
        updatePinDots()

        // Auto-verify khi đủ 4 ký tự
        if (pinBuffer.length == maxPinLength) {
            verifyPin()
        }
    }

    private fun onBackspacePressed() {
        if (pinBuffer.isEmpty()) return
        pinBuffer.deleteCharAt(pinBuffer.length - 1)
        updatePinDots()
        // Xóa error message khi người dùng sửa
        hideError()
    }

    /**
     * Xác thực PIN với repository.
     * - Đúng → finish() (TASK-47 sẽ navigate về MainActivity)
     * - Sai → shake animation + ghi nhận lần thất bại + kiểm tra lockout
     */
    private fun verifyPin() {
        val inputPin = pinBuffer.toString()

        if (pinRepo.verifyPin(inputPin)) {
            // ✅ PIN đúng
            onPinSuccess()
        } else {
            // ❌ PIN sai
            val remainingAttempts = Constants.MAX_PIN_ATTEMPTS - pinRepo.recordFailedAttempt()

            if (pinRepo.isLockedOut()) {
                startLockoutCountdown()
            } else {
                val errMsg = getString(R.string.pin_error_wrong_vi, remainingAttempts)
                showError(errMsg)
            }

            shakePinIndicator()
            // Reset buffer sau shake animation
            binding.pinIndicatorContainer.postDelayed({
                pinBuffer.clear()
                updatePinDots()
            }, 400L)
        }
    }

    private fun onPinSuccess() {
        pinRepo.resetFailedAttempts()
        // Đặt result OK để caller biết unlock thành công
        setResult(RESULT_OK)
        finish()
    }

    // ──────────────────────────────────────────────────────────────────────
    // UI — PIN Dots
    // ──────────────────────────────────────────────────────────────────────

    private val pinDots by lazy {
        listOf(binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4)
    }

    /**
     * Cập nhật 4 chấm: chấm có index < pinBuffer.length → filled (xanh),
     * còn lại → empty (viền xám).
     */
    private fun updatePinDots() {
        pinDots.forEachIndexed { index, dot ->
            val filled = index < pinBuffer.length
            dot.background = if (filled) {
                getDrawable(R.drawable.bg_pin_dot_filled)
            } else {
                getDrawable(R.drawable.bg_pin_dot_empty)
            }
            // Scale animation: chấm được fill sẽ scale lên 1.1 nhẹ
            dot.animate()
                .scaleX(if (filled) 1.1f else 1.0f)
                .scaleY(if (filled) 1.1f else 1.0f)
                .setDuration(120)
                .start()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // UI — Animations
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Scale-down animation khi nhấn phím (khớp keypad-btn:active trong design).
     */
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

    /**
     * Shake animation cho vùng chấm PIN khi nhập sai
     * (khớp @keyframes shake trong design HTML).
     */
    private fun shakePinIndicator() {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake_pin)
        binding.pinIndicatorContainer.startAnimation(shake)
    }

    // ──────────────────────────────────────────────────────────────────────
    // UI — Error & Lockout
    // ──────────────────────────────────────────────────────────────────────

    private fun showError(message: String) {
        binding.tvErrorMessage.text = message
        binding.tvErrorMessage.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvErrorMessage.visibility = View.INVISIBLE
    }

    /**
     * Kiểm tra nếu đang bị lockout khi resume — hiển thị countdown ngay.
     */
    private fun checkLockoutOnResume() {
        if (pinRepo.isLockedOut()) {
            startLockoutCountdown()
        }
    }

    /**
     * Bắt đầu đếm ngược lockout. Disable toàn bộ keypad trong thời gian lockout.
     */
    private fun startLockoutCountdown() {
        val remaining = pinRepo.getLockoutEndTime() - System.currentTimeMillis()
        if (remaining <= 0) {
            pinRepo.resetFailedAttempts()
            return
        }

        setKeypadEnabled(false)
        pinBuffer.clear()
        updatePinDots()

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
        binding.btn1.isEnabled = enabled
        binding.btn0.isEnabled = enabled
        binding.btnBackspace.isEnabled = enabled
        binding.btnFingerprint.isEnabled = enabled

        // Keys 2–9
        listOf(
            binding.keyBtn2.root, binding.keyBtn3.root,
            binding.keyBtn4.root, binding.keyBtn5.root, binding.keyBtn6.root,
            binding.keyBtn7.root, binding.keyBtn8.root, binding.keyBtn9.root
        ).forEach { it.isEnabled = enabled }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Dialogs
    // ──────────────────────────────────────────────────────────────────────

    private fun showForgotPinDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.pin_forgot_dialog_title)
            .setMessage(R.string.pin_forgot_dialog_message)
            .setPositiveButton(R.string.pin_forgot_dialog_ok, null)
            .show()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Back press — Không cho back về app khi đang ở màn hình PIN
    // ──────────────────────────────────────────────────────────────────────

    @Deprecated("Use OnBackPressedDispatcher", level = DeprecationLevel.WARNING)
    override fun onBackPressed() {
        // Chặn back press — người dùng phải nhập đúng PIN mới thoát
        // TASK-47 sẽ quyết định flow điều hướng chính xác
    }

    // ──────────────────────────────────────────────────────────────────────
    // Companion
    // ──────────────────────────────────────────────────────────────────────

    companion object {
        /**
         * Tạo Intent để start PinLockActivity.
         */
        fun createIntent(context: Context): Intent =
            Intent(context, PinLockActivity::class.java)

        /**
         * Shorthand start.
         */
        fun start(context: Context) {
            context.startActivity(createIntent(context))
        }
    }
}
