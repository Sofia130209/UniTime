package com.example.unitime

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.unitime.db.auth.User
import com.example.unitime.db.auth.UsersDbProvider
import com.example.unitime.utils.SessionManager
import com.example.unitime.utils.UserManager
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toast.makeText(
            this,
            "LoginActivity запущена",
            Toast.LENGTH_LONG
        ).show()

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

                val user = User(
                    login = login,
                    password = password
                )

                lifecycleScope.launch {
                    val isLoggedIn = userManager.logUser(user)

                    if (isLoggedIn) {
                        // * сохраняем сессию, чтобы при повторном запуске сразу открывался главный экран
                        SessionManager(this@LoginActivity).saveSession(login)

                        val session = SessionManager(this@LoginActivity)
                        session.saveSession(login)

                        Toast.makeText(
                            this@LoginActivity,
                            "Сессия сохранена: ${session.isLoggedIn}",
                            Toast.LENGTH_LONG
                        ).show()

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