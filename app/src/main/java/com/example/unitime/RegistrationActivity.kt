package com.example.unitime

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
import com.example.unitime.db.auth.User
import com.example.unitime.db.auth.UsersDbProvider
import com.example.unitime.db.auth.saveImageToInternalStorage
import com.example.unitime.utils.SessionManager
import com.example.unitime.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val loginField = findViewById<EditText>(R.id.editTextName)
        val passwordField = findViewById<EditText>(R.id.editTextPass)
        val btnReg = findViewById<Button>(R.id.btnRegister)

        avatarField.setOnClickListener {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        btnReg.setOnClickListener {
            val login = loginField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!validatePassword(password, passwordField)) return@setOnClickListener

            val userManager = UserManager(db)

            lifecycleScope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    selectedImage?.let { uri ->
                        saveImageToInternalStorage(applicationContext, uri)
                    }
                }

                val user = User(
                    login = login,
                    password = password,
                    avatarPath = savedPath
                )

                val isReg = userManager.regUser(user)
                if (isReg) {
                    // * раз пользователь зарегистрировался и данные у него заведомо верные —
                    // * сразу сохраняем сессию, чтобы главный экран открылся без повторного входа
                    SessionManager(this@RegistrationActivity).saveSession(login)

                    val intent = Intent(this@RegistrationActivity, MainActivity::class.java)
                    startActivity(intent)

                    finish()
                }
                else {
                    Toast.makeText(this@RegistrationActivity, "Такой логин уже занят", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}