package com.example.unitime.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.unitime.ui.MainActivity
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.local.model.User
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.managers.UserManager
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.log_activity)

        val db = UsersDbProvider.get(this)

        val loginField = findViewById<EditText>(R.id.editTextLogin)
        val passwordField = findViewById<EditText>(R.id.editTextPassword)

        val btnToReg = findViewById<Button>(R.id.btnToReg)
        btnToReg.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)

            finish()
        }

        val logBtn = findViewById<Button>(R.id.btnLogin)
        logBtn.setOnClickListener {
            val login = loginField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_LONG).show()
            }
            else {
                val userManager = UserManager(db)

                lifecycleScope.launch {
                    val isLoggedIn = userManager.logUser(login, password)

                    if (isLoggedIn) {
                        SessionManager(this@LoginActivity).saveSession(login)

                        val session = SessionManager(this@LoginActivity)
                        session.saveSession(login)

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)

                        finish()
                    }
                    else {
                        Toast.makeText(this@LoginActivity, "Введены неверный данные", Toast.LENGTH_LONG).show()

                        loginField.text.clear()
                        passwordField.text.clear()
                    }
                }
            }
        }
    }
}