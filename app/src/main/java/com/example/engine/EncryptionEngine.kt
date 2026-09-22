package com.example.engine

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionEngine {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH_BIT = 256

    private val secureRandom = SecureRandom()

    // Default master salt for app-level local vault isolation
    private val DEFAULT_VAULT_PASS = "ScannerPro_SecureVault_2026_Key"

    fun deriveKey(passcode: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passcode.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BIT)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    fun encryptBytes(data: ByteArray, passcode: String = DEFAULT_VAULT_PASS): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTE).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH_BYTE).apply { secureRandom.nextBytes(this) }
        val secretKey = deriveKey(passcode, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))
        val encryptedData = cipher.doFinal(data)

        // Result format: [Salt (16 bytes)] + [IV (12 bytes)] + [Encrypted Data]
        val result = ByteArray(salt.size + iv.size + encryptedData.size)
        System.arraycopy(salt, 0, result, 0, salt.size)
        System.arraycopy(iv, 0, result, salt.size, iv.size)
        System.arraycopy(encryptedData, 0, result, salt.size + iv.size, encryptedData.size)
        return result
    }

    fun decryptBytes(encryptedWithHeaders: ByteArray, passcode: String = DEFAULT_VAULT_PASS): ByteArray {
        if (encryptedWithHeaders.size < SALT_LENGTH_BYTE + IV_LENGTH_BYTE) {
            throw IllegalArgumentException("Invalid encrypted payload size")
        }

        val salt = ByteArray(SALT_LENGTH_BYTE)
        val iv = ByteArray(IV_LENGTH_BYTE)
        val cipherLength = encryptedWithHeaders.size - SALT_LENGTH_BYTE - IV_LENGTH_BYTE
        val ciphertext = ByteArray(cipherLength)

        System.arraycopy(encryptedWithHeaders, 0, salt, 0, SALT_LENGTH_BYTE)
        System.arraycopy(encryptedWithHeaders, SALT_LENGTH_BYTE, iv, 0, IV_LENGTH_BYTE)
        System.arraycopy(encryptedWithHeaders, SALT_LENGTH_BYTE + IV_LENGTH_BYTE, ciphertext, 0, cipherLength)

        val secretKey = deriveKey(passcode, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))
        return cipher.doFinal(ciphertext)
    }

    fun encryptFile(sourceFile: File, destEncryptedFile: File, passcode: String = DEFAULT_VAULT_PASS) {
        val rawBytes = sourceFile.readBytes()
        val encrypted = encryptBytes(rawBytes, passcode)
        destEncryptedFile.writeBytes(encrypted)
    }

    fun decryptFile(sourceEncryptedFile: File, destDecryptedFile: File, passcode: String = DEFAULT_VAULT_PASS) {
        val encrypted = sourceEncryptedFile.readBytes()
        val decrypted = decryptBytes(encrypted, passcode)
        destDecryptedFile.writeBytes(decrypted)
    }
}
