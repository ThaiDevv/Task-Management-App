package com.team.taskmanagementapp.ui.base

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.team.taskmanagementapp.ui.pin.PinLockActivity
import com.team.taskmanagementapp.util.PinManager

/**
 * Base Activity that handles PIN lock flow and auto-lock functionality.
 * Uses ProcessLifecycleOwner + DefaultLifecycleObserver to detect app background/foreground transitions.
 *
 * Auto-lock logic:
 * - Cold Start (Mở lại app sau khi thoát/xóa đa nhiệm): Luôn bắt buộc nhập PIN nếu PIN đã bật.
 * - Warm Resume (Ẩn app xuống nền khi đang dùng):
 *   - Nếu ẩn quá thời gian chờ (1 phút / AUTO_LOCK_TIMEOUT_MS) -> Tự động khóa và yêu cầu nhập lại PIN.
 *   - Nếu ẩn dưới 1 phút -> Cho phép tiếp tục sử dụng mà không cần nhập lại PIN.
 */
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var pinManager: PinManager
    private var pendingPinMode: PinLockActivity.PinMode? = null

    // Activity result launcher for PIN verification
    private val pinLockLauncher: ActivityResultLauncher<android.content.Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                isAppUnlockedInSession = true
                backgroundTimestamp = 0L
            }
            pendingPinMode = null
        }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            // Khi app quay lại foreground từ background
            if (backgroundTimestamp > 0L && isAppUnlockedInSession) {
                val timeInBackground = System.currentTimeMillis() - backgroundTimestamp
                val autoLockTimeout = pinManager.getAutoLockTimeout()
                if (timeInBackground > autoLockTimeout) {
                    // Đã ở background quá 1 phút -> khóa lại session!
                    isAppUnlockedInSession = false
                }
            }
            backgroundTimestamp = 0L
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            // Ghi nhận thời điểm app bị đưa xuống background
            backgroundTimestamp = System.currentTimeMillis()
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
        // Kiểm tra xem có cần yêu cầu nhập PIN không
        if (pinManager.isPinEnabled() && !isAppUnlockedInSession && pendingPinMode == null) {
            launchPinLock(PinLockActivity.PinMode.ENTER)
        }
    }

    /**
     * Launch PinLockActivity for the given mode.
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

    companion object {
        /**
         * Trạng thái mở khóa trong phiên chạy hiện tại (in-memory).
         * Khi ứng dụng bị kill / xóa khỏi đa nhiệm, biến này tự động reset về false!
         */
        @Volatile
        var isAppUnlockedInSession: Boolean = false

        /**
         * Thời điểm app đi vào background.
         */
        @Volatile
        private var backgroundTimestamp: Long = 0L
    }
}
