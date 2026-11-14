package com.example.unitime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.unitime.utils.SessionManager

/**
 * Точка входа. Проверяет сессию и решает, какой экран открыть:
 *  - главный (если пользователь уже вошёл),
 *  - экран входа (в противном случае).
 *
 * Почему отдельный экран: раньше проверка сессии делалась внутри LoginActivity,
 * и при крахе MainActivity система пересоздавала стек активностей —
 * пользователь снова видел экран регистрации/входа. Здесь же MainActivity
 * запускается напрямую, без промежуточного "запоминания" экрана входа.
 */
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val target = if (SessionManager(this).isLoggedIn) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        startActivity(target)
        finish()
    }
}
