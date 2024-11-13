package com.theimpartialai.speechScribe.managers

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedCredentialManager(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // TODO: Implement when the UI logic is ready
    fun saveCredentials(username: String, password: String, newPassword: String) {
        sharedPrefs.edit()
            .putString("username", username)
            .putString("password", password)
            .putString("new password", newPassword)
            .apply()
    }

    fun getUsername(): String? = sharedPrefs.getString("username", null)
    fun getPassword(): String? = sharedPrefs.getString("password", null)
    fun getNewPassword(): String? = sharedPrefs.getString("new password", null)
}