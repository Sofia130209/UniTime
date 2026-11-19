package com.example.unitime.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.local.model.User
import com.example.unitime.data.local.storage.saveImageToInternalStorage
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.managers.UserManager
import com.example.unitime.data.remote.RetrofitClient
import com.example.unitime.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class RegistrationActivity : ComponentActivity() {
    private var selectedImage: Uri? = null
    private lateinit var avatarField: ImageView

    private fun validatePassword(password: String, userPassword: EditText): Boolean {
        if (password.length < 4) {
            Toast.makeText(this, "Пароль должен содержать минимум 4 символа", Toast.LENGTH_LONG).show()
            userPassword.text.clear()
            return false
        }
        return true
    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedImage = uri
                avatarField.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reg_activity)

        val db = UsersDbProvider.get(this)

        avatarField = findViewById(R.id.imageViewAvatar)
        val loginField = findViewById<EditText>(R.id.editTextLogin)
        val nameField = findViewById<EditText>(R.id.editTextName)
        val surnameField = findViewById<EditText>(R.id.editTextSurname)
        val emailField = findViewById<EditText>(R.id.editTextEmail)
        val passwordField = findViewById<EditText>(R.id.editTextPass)
        val btnReg = findViewById<Button>(R.id.btnRegister)

        avatarField.setOnClickListener {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        btnReg.setOnClickListener {
            val login = loginField.text.toString().trim()
            val name = nameField.text.toString().trim()
            val surname = surnameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (login.isEmpty() || password.isEmpty() || name.isEmpty() || surname.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Все основные поля должны быть заполнены", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!validatePassword(password, passwordField)) return@setOnClickListener

            // Блокируем кнопку, чтобы пользователь не нажал её дважды
            btnReg.isEnabled = false
            btnReg.text = "Регистрация..."

            lifecycleScope.launch {
                try {
                    // 1. ОТПРАВЛЯЕМ ДАННЫЕ НА СЕРВЕР
                    val response = RetrofitClient.api.registerUser(
                        login = login,
                        password = password,
                        email = email,
                        name = name,
                        surname = surname
                    )

                    if (response.isSuccessful && response.body()?.success == true) {
                        val savedPath = withContext(Dispatchers.IO) {
                            selectedImage?.let { uri ->
                                saveImageToInternalStorage(applicationContext, uri)
                            }
                        }

                        val user = User(
                            login = login,
                            name = name,
                            surname = surname,
                            email = email,
                            password = password,
                            avatarPath = savedPath
                        )

                        val userManager = UserManager(db)
                        val isRegLocal = userManager.regUser(user)

                        if (isRegLocal) {
                            // 3. Если есть аватарка, загружаем её на сервер асинхронно
                            if (selectedImage != null && savedPath != null) {
                                uploadAvatarToServer(login, savedPath)
                            }

                            // 4. Сохраняем сессию и переходим в приложение
                            SessionManager(this@RegistrationActivity).saveSession(login)

                            val intent = Intent(this@RegistrationActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@RegistrationActivity, "Ошибка локального сохранения", Toast.LENGTH_LONG).show()
                            btnReg.isEnabled = true
                            btnReg.text = "Зарегистрироваться"
                        }
                    } else {
                        // Обработка ошибок от сервера (например, "Login already taken")
                        val errorMsg = response.body()?.error ?: "Ошибка регистрации на сервере"
                        Toast.makeText(this@RegistrationActivity, errorMsg, Toast.LENGTH_LONG).show()
                        btnReg.isEnabled = true
                        btnReg.text = "Зарегистрироваться"
                    }
                } catch (e: Exception) {
                    // Если нет интернета или другая ошибка сети
                    Toast.makeText(this@RegistrationActivity, "Ошибка сети: проверьте подключение", Toast.LENGTH_LONG).show()
                    btnReg.isEnabled = true
                    btnReg.text = "Зарегистрироваться"
                }
            }
        }
    }

    // Отдельный метод для загрузки аватарки на сервер
    private suspend fun uploadAvatarToServer(login: String, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
            val loginBody = login.toRequestBody("text/plain".toMediaTypeOrNull())

            // Вызываем эндпоинт загрузки аватара
            RetrofitClient.api.uploadAvatar(
                login = loginBody,
                avatar = body
            )
        } catch (e: Exception) {
            // Если загрузка аватара не удалась, мы не прерываем регистрацию,
            // так как аккаунт уже создан. Просто логируем ошибку.
            e.printStackTrace()
        }
    }
}