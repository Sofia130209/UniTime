package com.example.unitime.ui.auth

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.ui.BottomNavigationHelper
import kotlinx.coroutines.launch
import java.io.File

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        BottomNavigationHelper.setup(this, R.id.nav_profile)

        val session = SessionManager(this)

        val avatarField = findViewById<ImageView>(R.id.profileAvatar)
        val nameText = findViewById<TextView>(R.id.profileName)
        val loginText = findViewById<TextView>(R.id.profileLogin)
        val emailText = findViewById<TextView>(R.id.profileEmail)

        val login = session.userLogin

        if (login == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = UsersDbProvider.get(this)
        val usersDao = db.getUsersDao()

        lifecycleScope.launch {
            val user = usersDao.getUserByLogin(login)

            if (user != null) {
                nameText.text = "${user.name} ${user.surname}"
                loginText.text = "@${user.login}"
                emailText.text = user.email

                val avatarFile = user.avatarPath?.let(::File)?.takeIf { it.exists() }
                if (avatarFile != null) {
                    avatarField.load(avatarFile) {
                        crossfade(true)
                        placeholder(R.drawable.ic_profile_placeholder)
                        error(R.drawable.ic_profile_placeholder)
                        transformations(CircleCropTransformation())
                    }
                } else {
                    avatarField.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                Toast.makeText(this@ProfileActivity, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            }
        }
    }
}