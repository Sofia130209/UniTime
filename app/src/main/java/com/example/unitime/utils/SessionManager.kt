package com.example.unitime.utils

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit { putBoolean("is_logged_in", value) }

    var userLogin: String?
        get() = prefs.getString("user_login", null)
        set(value) = prefs.edit { putString("user_login", value).apply() }

    fun saveSession(login: String) {
        // * commit() вместо apply(): запись гарантированно успевает сохраниться на диск
        // * до того, как откроется главный экран (apply() пишет асинхронно и мог потерять сессию)
        prefs.edit(commit = true) { putBoolean("is_logged_in", true).putString("user_login", login) }
    }

    fun clear() {
        prefs.edit { clear().apply() }
    }
}