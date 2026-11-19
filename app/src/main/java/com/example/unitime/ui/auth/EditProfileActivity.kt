package com.example.unitime.ui.auth

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.managers.UserManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var nameField: EditText
    private lateinit var surnameField: EditText
    private lateinit var loginField: EditText
    private lateinit var emailField: EditText

    private lateinit var telegramField: EditText
    private lateinit var phoneField: EditText

    private lateinit var currentPasswordField: EditText
    private lateinit var newPasswordField: EditText
    private lateinit var repeatPasswordField: EditText

    private lateinit var saveButton: MaterialButton
    private lateinit var avatarImageView: ImageView

    private var selectedImageUri: Uri? = null
    private var oldAvatarPath: String? = null // Для удаления старой аватарки

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            avatarImageView.load(uri) {
                crossfade(true)
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder)
                transformations(CircleCropTransformation())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)

        nameField = findViewById(R.id.editName)
        surnameField = findViewById(R.id.editSurname)
        loginField = findViewById(R.id.editLogin)
        emailField = findViewById(R.id.editEmail)

        telegramField = findViewById(R.id.editTelegram)
        phoneField = findViewById(R.id.editPhone)

        currentPasswordField = findViewById(R.id.editCurrentPassword)
        newPasswordField = findViewById(R.id.editNewPassword)
        repeatPasswordField = findViewById(R.id.editRepeatPassword)

        saveButton = findViewById(R.id.saveProfileButton)
        avatarImageView = findViewById(R.id.editAvatarImage)

        avatarImageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

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
                Toast.makeText(this@EditProfileActivity, "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            nameField.setText(user.name)
            surnameField.setText(user.surname)
            loginField.setText(user.login)
            emailField.setText(user.email)

            telegramField.setText(user.telegram ?: "")
            phoneField.setText(user.phone ?: "")

            oldAvatarPath = user.avatarPath // Сохраняем путь к старой аватарке

            val avatarFile = user.avatarPath?.let(::File)?.takeIf { it.exists() }
            if (avatarFile != null) {
                avatarImageView.load(avatarFile) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile_placeholder)
                    error(R.drawable.ic_profile_placeholder)
                    transformations(CircleCropTransformation())
                }
            } else {
                avatarImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    private fun saveChanges() {
        val name = nameField.text.toString().trim()
        val surname = surnameField.text.toString().trim()
        val login = loginField.text.toString().trim()
        val email = emailField.text.toString().trim()

        val telegram = telegramField.text.toString().trim()
        val phone = phoneField.text.toString().trim()

        val currentPassword = currentPasswordField.text.toString().trim()
        val newPassword = newPasswordField.text.toString().trim()
        val repeatPassword = repeatPasswordField.text.toString().trim()

        if (name.isEmpty() || surname.isEmpty() || login.isEmpty() || email.isEmpty()) {
            showMessage("Заполните все основные поля")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Введите корректный email")
            return
        }

        val changingPassword = currentPassword.isNotEmpty() || newPassword.isNotEmpty() || repeatPassword.isNotEmpty()

        if (changingPassword) {
            if (currentPassword.isEmpty() || newPassword.isEmpty() || repeatPassword.isEmpty()) {
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

                    if (login != oldLogin && dao.isLoginTaken(login, user.id)) {
                        return@withContext "Этот логин уже занят"
                    }

                    val userManager = UserManager(UsersDbProvider.get(applicationContext))
                    var passwordHash = user.password

                    if (changingPassword) {
                        if (userManager.hashPassword(currentPassword) != user.password) {
                            return@withContext "Неверный текущий пароль"
                        }
                        passwordHash = userManager.hashPassword(newPassword)
                    }

                    var newAvatarPath = user.avatarPath

                    // Если выбрана новая аватарка, сохраняем её и удаляем старую
                    if (selectedImageUri != null) {
                        val savedPath = saveImageToInternalStorage(selectedImageUri!!)
                        if (savedPath != null) {
                            newAvatarPath = savedPath
                            // Удаляем старую аватарку, чтобы не засорять память
                            oldAvatarPath?.let { File(it) }?.takeIf { it.exists() }?.delete()
                        }
                    }

                    dao.update(
                        user.copy(
                            name = name,
                            surname = surname,
                            login = login,
                            email = email,
                            password = passwordHash,
                            avatarPath = newAvatarPath,
                            telegram = telegram.ifBlank { null },
                            phone = phone.ifBlank { null }
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
                showMessage("Ошибка при сохранении профиля: ${e.message}")
            } finally {
                saveButton.isEnabled = true
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "avatar_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}