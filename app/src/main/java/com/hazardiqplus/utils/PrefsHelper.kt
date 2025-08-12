package com.hazardiqplus.utils

import android.content.Context
import androidx.core.content.edit

object PrefsHelper {
    private const val PREFS_NAME = "HazardIQPrefs"
    private const val KEY_ROLE = "user_role"

    fun saveUserRole(context: Context, role: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_ROLE, role) }
    }

    fun getUserRole(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ROLE, null)
    }
}