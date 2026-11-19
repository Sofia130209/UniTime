package com.example.unitime.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.local.model.User
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.remote.RetrofitClient
import com.example.unitime.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.log_activity)

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
                return@setOnClickListener
            }

            // 1. Блокируем кнопку и меняем текст, чтобы показать процесс загрузки
            logBtn.isEnabled = false
            logBtn.text = "Вход..."

            lifecycleScope.launch {
                try {
                    // 2. Делаем запрос на сервер
                    val response = RetrofitClient.api.loginUser(login, password)

                    // 3. Проверяем успешный ответ
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverUser = response.body()?.user

                        if (serverUser != null) {
                            // 4. Сохраняем сессию
                            SessionManager(this@LoginActivity).saveSession(serverUser.login)

                            // 5. Синхронизируем данные с локальной базой Room (для быстрой работы UI)
                            withContext(Dispatchers.IO) {
                                val localUser = User(
                                    login = serverUser.login,
                                    name = serverUser.name ?: "",
                                    surname = serverUser.surname ?: "",
                                    email = serverUser.email ?: "",
                                    password = password, // Сохраняем введенный пароль для локальной совместимости
                                    telegram = serverUser.telegram,
                                    phone = serverUser.phone,
                                    avatarPath = null // Аватар можно будет обновить при первом открытии профиля
                                )

                                // Вставляем или обновляем пользователя в локальной БД
                                UsersDbProvider.get(this@LoginActivity)
                                    .getUsersDao()
                                    .insert(localUser)
                            }

                            // 6. Переходим в главное приложение
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            showError("Ошибка получения данных пользователя")
                        }
                    } else {
                        // Сервер вернул ошибку (например, неверный пароль)
                        val errorMsg = response.body()?.error ?: "Неверный логин или пароль"
                        showError(errorMsg)
                    }
                } catch (e: Exception) {
                    // Ошибка сети (нет интернета или сервер недоступен)
                    showError("Ошибка сети. Проверьте подключение к интернету.")
                } finally {
                    // 7. Возвращаем кнопку в исходное состояние в любом случае
                    logBtn.isEnabled = true
                    logBtn.text = "Войти"
                }
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}