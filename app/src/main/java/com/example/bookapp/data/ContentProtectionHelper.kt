package com.example.bookapp.data

import android.content.Context
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Viewer-only packaged content protection.
 *
 * The viewer no longer ships its source JSON as plaintext. Content is stored as
 * AES-GCM ciphertext and authenticated before it is passed to the content merger.
 * This is strong protection against casual APK asset extraction/tampering, but
 * not absolute secrecy against a fully compromised/rooted device because the
 * running app must eventually decrypt content to display it.
 */
object ContentProtectionHelper {
    private const val NONCE_SIZE = 12
    private const val TAG = "tazieh-viewer-v1"

    // Split fragments avoid keeping one obvious plaintext key in the asset itself.
    // For stronger confidentiality than local APK protection, move the content key
    // to a server-side provisioning system in a future stage.
    private val key: ByteArray by lazy {
        val material = listOf("Tz9!", "ieh#", "2026", "Viewer-Protection", "Fa").joinToString("")
        MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
    }

    fun readProtectedJson(context: Context, assetName: String): String {
        val encrypted = context.assets.open("content/$assetName").use { it.readBytes() }
        require(encrypted.size > NONCE_SIZE) { "فایل محتوای محافظت‌شده نامعتبر است" }

        val nonce = encrypted.copyOfRange(0, NONCE_SIZE)
        val ciphertext = encrypted.copyOfRange(NONCE_SIZE, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }
}
