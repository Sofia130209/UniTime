package com.example.unitime.ui.auth

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.ui.BottomNavigationHelper
import kotlinx.coroutines.launch

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        BottomNavigationHelper.setup(this, R.id.nav_profile)

        val nameText = findViewById<TextView>(R.id.profileName)
        val loginText = findViewById<TextView>(R.id.profileLogin)
        val emailText = findViewById<TextView>(R.id.profileEmail)

        val session = SessionManager(this)
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
            } else {
                Toast.makeText(
                    this@ProfileActivity,
                    "Пользователь не найден",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}