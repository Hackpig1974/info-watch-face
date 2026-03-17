package com.damon1974.infowatchface.phone

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {

    private const val FILE_NAME = "secure_prefs"
    private const val KEY_SESSION = "session_key"
    private const val KEY_ORG_ID = "org_id"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(context: Context, sessionKey: String, orgId: String) {
        prefs(context).edit().putString(KEY_SESSION, sessionKey).putString(KEY_ORG_ID, orgId).apply()
    }

    fun getSessionKey(context: Context): String? = prefs(context).getString(KEY_SESSION, null)
    fun getOrgId(context: Context): String? = prefs(context).getString(KEY_ORG_ID, null)
    fun isLoggedIn(context: Context) = getSessionKey(context) != null

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
