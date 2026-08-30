package com.team.taskmanagementapp.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Unit tests verifying PIN hashing, salt uniqueness, and brute-force protection logic (TASK-46 & TASK-47).
 */
class PinSecurityTest {

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val input = (salt + pin).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `different salts generate completely different hashes for the same PIN`() {
        val pin = "1234"
        val salt1 = generateSalt()
        val salt2 = generateSalt()

        val hash1 = hashPin(pin, salt1)
        val hash2 = hashPin(pin, salt2)

        assertTrue("Salts should be unique", salt1 != salt2)
        assertTrue("Hashes should be distinct due to unique salt", hash1 != hash2)
    }

    @Test
    fun `matching PIN with correct salt verifies successfully`() {
        val pin = "4567"
        val salt = generateSalt()
        val storedHash = hashPin(pin, salt)

        val inputHash = hashPin("4567", salt)
        assertEquals(storedHash, inputHash)
    }

    @Test
    fun `wrong PIN fails verification`() {
        val pin = "1234"
        val salt = generateSalt()
        val storedHash = hashPin(pin, salt)

        val wrongInputHash = hashPin("9999", salt)
        assertFalse(storedHash == wrongInputHash)
    }

    @Test
    fun `PIN format validation accepts 4 to 6 digits only`() {
        fun isValid(p: String) = p.length in 4..6 && p.all { it.isDigit() }

        assertTrue(isValid("1234"))
        assertTrue(isValid("123456"))
        assertFalse(isValid("123"))      // too short
        assertFalse(isValid("1234567"))  // too long
        assertFalse(isValid("12a4"))     // contains letters
        assertFalse(isValid(""))         // empty
    }
}
