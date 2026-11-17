package com.example.unitime.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.ui.auth.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        lifecycleScope.launch {
            delay(600)
            val sessionManager = SessionManager(this@SplashActivity)
            val isLoggedIn = sessionManager.isLoggedIn
            val targetActivity = if (isLoggedIn) MainActivity::class.java
            else LoginActivity::class.java
            startActivity(Intent(this@SplashActivity, targetActivity))
            finish()
            keepSplash = false
        }
    }
}