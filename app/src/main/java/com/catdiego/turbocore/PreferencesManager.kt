package com.catdiego.turbocore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    private val sharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "turbo_core_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback or handle error (e.g. clear corrupted prefs)
        context.getSharedPreferences("turbo_core_prefs_fallback", Context.MODE_PRIVATE)
    }

    fun saveLastProfile(profileName: String) {
        sharedPreferences.edit().putString(KEY_LAST_PROFILE, profileName).apply()
    }

    fun getLastProfile(): String {
        return sharedPreferences.getString(KEY_LAST_PROFILE, "Balanceado") ?: "Balanceado"
    }

    companion object {
        private const val KEY_LAST_PROFILE = "last_profile_name"
    }
}
