package com.team.taskmanagementapp.util

import android.content.Context
import com.team.taskmanagementapp.pinRepository
import com.team.taskmanagementapp.security.PinRepository

/**
 * PinManager — Adapter / Utility wrapper around [PinRepository]
 * ensuring backward compatibility for TMA-47 and all callers.
 */
class PinManager private constructor(context: Context) {

    private val pinRepo: PinRepository = context.applicationContext.pinRepository()

    /**
     * Sets a new PIN.
     * @param pin The PIN to set (4-6 digits)
     * @return true if valid and set successfully
     */
    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        pinRepo.setPin(pin)
        return true
    }

    /**
     * Verifies if the input PIN matches the stored PIN.
     */
    fun verifyPin(inputPin: String): Boolean {
        return pinRepo.verifyPin(inputPin)
    }

    /**
     * Checks if PIN lock is enabled.
     */
    fun isPinEnabled(): Boolean {
        return pinRepo.isPinEnabled()
    }

    /**
     * Removes the stored PIN (disables PIN lock).
     */
    fun removePin() {
        pinRepo.clearPin()
    }

    /**
     * Gets the auto-lock timeout in milliseconds.
     */
    fun getAutoLockTimeout(): Long {
        return pinRepo.getAutoLockTimeout()
    }

    /**
     * Sets the auto-lock timeout.
     */
    fun setAutoLockTimeout(timeoutMs: Long) {
        pinRepo.setAutoLockTimeout(timeoutMs)
    }

    /**
     * Validates PIN format (4-6 digits).
     */
    fun isValidPin(pin: String): Boolean {
        return pin.length in 4..6 && pin.all { it.isDigit() }
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
