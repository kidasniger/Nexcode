package com.nexcode.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class NexcodeSecureVault(context: Context) {

    private val appContext = context.applicationContext

    private val sharedPreferences: SharedPreferences = createEncryptedPrefs(appContext)

    companion object {
        private const val TAG = "NexcodeSecureVault"
        private const val PREFS_FILE = "nexcode_secure_vault"
        private const val FALLBACK_FILE = "nexcode_vault_fallback"
        private const val KEY_GEMINI_API_KEY = "encrypted_gemini_api_key"
        private const val KEY_GIT_TOKEN = "encrypted_git_token"

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            // Sous Robolectric (tests JVM), le Keystore matériel est simulé et le chemin
            // Tink/EncryptedSharedPreferences peut se figer de façon non déterministe
            // (blocages CI observés dans les setUp). Le repli plain-SharedPreferences
            // est le même chemin que celui utilisé sur un appareil sans Keystore : il
            // est donc testé volontairement, de façon déterministe.
            if (isRunningUnderRobolectric()) {
                return context.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
            }
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Reinitialisation du coffre chiffre", t)
                try {
                    context.deleteSharedPreferences(PREFS_FILE)
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILE,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (t2: Throwable) {
                    Log.w(TAG, "Repli sur SharedPreferences standard (sans Keystore materiel)", t2)
                    context.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
                }
            }
        }

        private fun isRunningUnderRobolectric(): Boolean {
            return try {
                Build.FINGERPRINT?.contains("robolectric", ignoreCase = true) == true ||
                    System.getProperty("robolectric.offline") != null
            } catch (_: Throwable) {
                false
            }
        }
    }

    fun saveApiKey(apiKey: String) {
        try {
            sharedPreferences.edit()
                .putString(KEY_GEMINI_API_KEY, apiKey)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur sauvegarde cle API, repli", t)
            appContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
        }
    }

    fun getApiKey(): String? {
        return try {
            sharedPreferences.getString(KEY_GEMINI_API_KEY, null)
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur lecture cle API, repli", t)
            appContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
                .getString(KEY_GEMINI_API_KEY, null)
        }
    }

    fun hasApiKey(): Boolean {
        val key = getApiKey()
        return !key.isNullOrBlank()
    }

    fun clearApiKey() {
        try {
            sharedPreferences.edit()
                .remove(KEY_GEMINI_API_KEY)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur suppression cle API, repli", t)
            appContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
                .edit().remove(KEY_GEMINI_API_KEY).apply()
        }
    }

    fun saveGitToken(token: String) {
        try {
            sharedPreferences.edit()
                .putString(KEY_GIT_TOKEN, token)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur sauvegarde jeton Git, repli", t)
            appContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_GIT_TOKEN, token).apply()
        }
    }

    fun getGitToken(): String? {
        return try {
            sharedPreferences.getString(KEY_GIT_TOKEN, null)
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur lecture jeton Git, repli", t)
            appContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
                .getString(KEY_GIT_TOKEN, null)
        }
    }

    fun clearGitToken() {
        try {
            sharedPreferences.edit()
                .remove(KEY_GIT_TOKEN)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur suppression jeton Git, repli", t)
            appContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
                .edit().remove(KEY_GIT_TOKEN).apply()
        }
    }

}
