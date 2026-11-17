package com.example.unitime.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.managers.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var nameField: EditText
    private lateinit var surnameField: EditText
    private lateinit var loginField: EditText
    private lateinit var emailField: EditText

    private lateinit var currentPasswordField: EditText
    private lateinit var newPasswordField: EditText
    private lateinit var repeatPasswordField: EditText

    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)

        nameField = findViewById(R.id.editName)
        surnameField = findViewById(R.id.editSurname)
        loginField = findViewById(R.id.editLogin)
        emailField = findViewById(R.id.editEmail)

        currentPasswordField = findViewById(R.id.editCurrentPassword)
        newPasswordField = findViewById(R.id.editNewPassword)
        repeatPasswordField = findViewById(R.id.editRepeatPassword)

        saveButton = findViewById(R.id.saveProfileButton)

        loadUser()

        saveButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadUser() {
        val login = sessionManager.userLogin ?: run {
            finish()
            return
        }

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                UsersDbProvider.get(applicationContext)
                    .getUsersDao()
                    .getUserByLogin(login)
            }

            if (user == null) {
                Toast.makeText(
                    this@EditProfileActivity,
                    "Не удалось загрузить профиль",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            nameField.setText(user.name)
            surnameField.setText(user.surname)
            loginField.setText(user.login)
            emailField.setText(user.email)
        }
    }

    private fun saveChanges() {
        val name = nameField.text.toString().trim()
        val surname = surnameField.text.toString().trim()
        val login = loginField.text.toString().trim()
        val email = emailField.text.toString().trim()

        val currentPassword = currentPasswordField.text.toString()
        val newPassword = newPasswordField.text.toString()
        val repeatPassword = repeatPasswordField.text.toString()

        if (name.isEmpty() || surname.isEmpty() ||
            login.isEmpty() || email.isEmpty()
        ) {
            showMessage("Заполните все основные поля")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Введите корректный email")
            return
        }

        val changingPassword =
            currentPassword.isNotEmpty() ||
                    newPassword.isNotEmpty() ||
                    repeatPassword.isNotEmpty()

        if (changingPassword) {
            if (currentPassword.isEmpty() ||
                newPassword.isEmpty() ||
                repeatPassword.isEmpty()
            ) {
                showMessage("Заполните все поля смены пароля")
                return
            }

            if (newPassword.length < 4) {
                showMessage("Пароль должен содержать минимум 4 символа")
                return
            }

            if (newPassword != repeatPassword) {
                showMessage("Новые пароли не совпадают")
                return
            }
        }

        saveButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val dao = UsersDbProvider.get(applicationContext).getUsersDao()
                val oldLogin = sessionManager.userLogin ?: return@launch

                val result = withContext(Dispatchers.IO) {
                    val user = dao.getUserByLogin(oldLogin)
                        ?: return@withContext "Пользователь не найден"

                    if (dao.isLoginTaken(login, user.id)) {
                        return@withContext "Этот логин уже занят"
                    }

                    val userManager = UserManager(
                        UsersDbProvider.get(applicationContext)
                    )

                    var passwordHash = user.password

                    if (changingPassword) {
                        if (userManager.hashPassword(currentPassword) != user.password) {
                            return@withContext "Неверный текущий пароль"
                        }

                        passwordHash = userManager.hashPassword(newPassword)
                    }

                    dao.update(
                        user.copy(
                            name = name,
                            surname = surname,
                            login = login,
                            email = email,
                            password = passwordHash
                        )
                    )

                    "SUCCESS"
                }

                if (result == "SUCCESS") {
                    sessionManager.userLogin = login

                    showMessage("Профиль успешно обновлён")
                    finish()
                } else {
                    showMessage(result)
                }

            } catch (e: Exception) {
                showMessage("Ошибка при сохранении профиля")
            } finally {
                saveButton.isEnabled = true
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
