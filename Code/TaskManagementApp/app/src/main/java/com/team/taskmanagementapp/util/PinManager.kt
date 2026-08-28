package com.team.taskmanagementapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

/**
 * Manages PIN storage and verification with secure hashing.
 * Uses SHA-256 with random salt and stores in EncryptedSharedPreferences.
 */
class PinManager private constructor(context: Context) {

    private val encryptedPrefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            Constants.ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Sets a new PIN. Generates a random salt, hashes the PIN, and stores both.
     * @param pin The PIN to set (4-6 digits)
     * @return true if successful
     */
    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false

        val salt = generateSalt()
        val hash = hashPin(pin, salt)

        encryptedPrefs.edit()
            .putString(Constants.KEY_PIN_HASH, hash)
            .putString(Constants.KEY_PIN_SALT, salt)
            .putBoolean(Constants.KEY_PIN_ENABLED, true)
            .apply()

        return true
    }

    /**
     * Verifies if the input PIN matches the stored PIN.
     * @param inputPin The PIN to verify
     * @return true if PIN matches, false otherwise
     */
    fun verifyPin(inputPin: String): Boolean {
        val storedHash = encryptedPrefs.getString(Constants.KEY_PIN_HASH, null) ?: return false
        val storedSalt = encryptedPrefs.getString(Constants.KEY_PIN_SALT, null) ?: return false

        val inputHash = hashPin(inputPin, storedSalt)
        return storedHash == inputHash
    }

    /**
     * Checks if PIN lock is enabled.
     * @return true if PIN is enabled
     */
    fun isPinEnabled(): Boolean {
        return encryptedPrefs.getBoolean(Constants.KEY_PIN_ENABLED, false)
    }

    /**
     * Removes the stored PIN (disables PIN lock).
     */
    fun removePin() {
        encryptedPrefs.edit()
            .remove(Constants.KEY_PIN_HASH)
            .remove(Constants.KEY_PIN_SALT)
            .putBoolean(Constants.KEY_PIN_ENABLED, false)
            .apply()
    }

    /**
     * Gets the auto-lock timeout in milliseconds.
     * @return Timeout in ms, default is AUTO_LOCK_TIMEOUT_MS (1 minute)
     */
    fun getAutoLockTimeout(): Long {
        return encryptedPrefs.getLong(Constants.KEY_AUTO_LOCK_TIMER, Constants.AUTO_LOCK_TIMEOUT_MS)
    }

    /**
     * Sets the auto-lock timeout.
     * @param timeoutMs Timeout in milliseconds
     */
    fun setAutoLockTimeout(timeoutMs: Long) {
        encryptedPrefs.edit()
            .putLong(Constants.KEY_AUTO_LOCK_TIMER, timeoutMs)
            .apply()
    }

    /**
     * Validates PIN format (4-6 digits).
     * @param pin The PIN to validate
     * @return true if valid
     */
    private fun isValidPin(pin: String): Boolean {
        return pin.length in 4..6 && pin.all { it.isDigit() }
    }

    /**
     * Generates a random salt for hashing.
     * @return Base64-encoded salt
     */
    private fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Hashes a PIN with salt using SHA-256.
     * @param pin The PIN to hash
     * @param salt The salt (Base64 encoded)
     * @return Base64-encoded hash
     */
    private fun hashPin(pin: String, salt: String): String {
        val saltedPin = "$salt$pin"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(saltedPin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    companion object {
        @Volatile
        private var instance: PinManager? = null

        fun getInstance(context: Context): PinManager {
            return instance ?: synchronized(this) {
                instance ?: PinManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
