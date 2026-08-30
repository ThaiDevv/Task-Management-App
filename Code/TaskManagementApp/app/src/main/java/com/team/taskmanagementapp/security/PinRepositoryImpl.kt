package com.team.taskmanagementapp.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.team.taskmanagementapp.util.Constants
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PinRepositoryImpl — Triển khai lưu trữ PIN bảo mật theo tiêu chuẩn:
 *
 *  1. Sinh Salt ngẫu nhiên 16 bytes bằng SecureRandom mỗi khi đặt PIN mới.
 *  2. Hash PIN bằng SHA-256(salt_hex + pin) — không lưu plaintext.
 *  3. Lưu hash và salt vào EncryptedSharedPreferences (AES-256-GCM / AES-256-SIV)
 *     được bảo vệ bởi Android Keystore — file được mã hóa ở cấp độ hệ thống.
 *  4. Xử lý brute-force lockout: khoá tạm 30 giây sau MAX_PIN_ATTEMPTS lần sai.
 *
 * Cách dùng:
 * ```
 * val pinRepo = PinRepositoryImpl.getInstance(context)
 * pinRepo.setPin("1234")
 * pinRepo.verifyPin("1234") // true
 * pinRepo.verifyPin("0000") // false
 * ```
 */
class PinRepositoryImpl private constructor(context: Context) : PinRepository {

    // EncryptedSharedPreferences — dùng AES256_SIV để mã hóa keys,
    // AES256_GCM để mã hóa values; backed bởi Android Keystore.
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context.applicationContext,
            Constants.ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // PIN Management
    // ──────────────────────────────────────────────────────────────────────

    override fun isPinEnabled(): Boolean =
        prefs.getBoolean(Constants.KEY_PIN_ENABLED, false)

    override fun setPin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)

        prefs.edit()
            .putString(Constants.KEY_PIN_SALT, salt)
            .putString(Constants.KEY_PIN_HASH, hash)
            .putBoolean(Constants.KEY_PIN_ENABLED, true)
            .apply()

        // Reset số lần nhập sai khi đặt PIN mới
        resetFailedAttempts()
    }

    override fun verifyPin(inputPin: String): Boolean {
        val salt = prefs.getString(Constants.KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(Constants.KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPin(inputPin, salt)
        return storedHash == inputHash
    }

    override fun clearPin() {
        prefs.edit()
            .remove(Constants.KEY_PIN_HASH)
            .remove(Constants.KEY_PIN_SALT)
            .putBoolean(Constants.KEY_PIN_ENABLED, false)
            .apply()
        resetFailedAttempts()
    }

    override fun setPinEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(Constants.KEY_PIN_ENABLED, enabled)
            .apply()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Brute-force Lockout
    // ──────────────────────────────────────────────────────────────────────

    override fun recordFailedAttempt(): Int {
        val current = getFailedAttempts() + 1
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current).apply()

        if (current >= Constants.MAX_PIN_ATTEMPTS) {
            val lockoutEnd = System.currentTimeMillis() + Constants.LOCKOUT_DURATION_MS
            prefs.edit().putLong(KEY_LOCKOUT_END_TIME, lockoutEnd).apply()
        }
        return current
    }

    override fun getFailedAttempts(): Int =
        prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    override fun isLockedOut(): Boolean {
        val lockoutEnd = getLockoutEndTime()
        return lockoutEnd > System.currentTimeMillis()
    }

    override fun getLockoutEndTime(): Long =
        prefs.getLong(KEY_LOCKOUT_END_TIME, 0L)

    override fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_END_TIME, 0L)
            .apply()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Cryptography Helpers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Sinh chuỗi Salt ngẫu nhiên 16 bytes (hex, 32 ký tự).
     * SecureRandom đảm bảo tính không đoán được (cryptographically secure).
     */
    private fun generateSalt(): String {
        val bytes = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    /**
     * Hash PIN theo SHA-256(salt + pin).
     * Việc prepend salt trước PIN giúp chống Rainbow Table Attack.
     *
     * @param pin Mã PIN plaintext.
     * @param salt Chuỗi salt hex.
     * @return Chuỗi hash hex (64 ký tự SHA-256).
     */
    private fun hashPin(pin: String, salt: String): String {
        val input = (salt + pin).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        return digest.digest(input).toHex()
    }

    /** Chuyển ByteArray sang chuỗi hex lowercase. */
    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    // ──────────────────────────────────────────────────────────────────────
    // Companion / Singleton
    // ──────────────────────────────────────────────────────────────────────

    companion object {
        private const val SALT_LENGTH_BYTES = 16
        private const val HASH_ALGORITHM = "SHA-256"

        // Keys nội bộ — không expose ra Constants để tránh trùng tên
        private const val KEY_FAILED_ATTEMPTS = "key_pin_failed_attempts"
        private const val KEY_LOCKOUT_END_TIME = "key_pin_lockout_end_time"

        @Volatile
        private var instance: PinRepositoryImpl? = null

        /**
         * Lấy singleton instance của PinRepositoryImpl.
         * Thread-safe với Double-Checked Locking.
         */
        fun getInstance(context: Context): PinRepositoryImpl =
            instance ?: synchronized(this) {
                instance ?: PinRepositoryImpl(context).also { instance = it }
            }
    }
}
