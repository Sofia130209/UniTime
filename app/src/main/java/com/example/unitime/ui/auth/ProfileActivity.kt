package com.example.unitime.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.ui.events.ProfileEventsAdapter
import com.example.unitime.data.local.db.events.EventsDbProvider

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        BottomNavigationHelper.setup(this, R.id.nav_profile)

        val session = SessionManager(this)
        val login = session.userLogin

        val eventsRecycler =
            findViewById<RecyclerView>(R.id.profileEventsRecycler)

        val emptyEventsText =
            findViewById<TextView>(R.id.emptyEventsText)

        val createdCount =
            findViewById<TextView>(R.id.profileCreatedCount)

        val eventsDb = EventsDbProvider.get(this)
        val eventsDao = eventsDb.getEventsDao()

        val adapter = ProfileEventsAdapter { event ->

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удалить мероприятие?")
                .setMessage(
                    "Вы действительно хотите удалить «${event.activityTitle}»? " +
                            "Это действие нельзя отменить."
                )
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить") { _, _ ->

                    lifecycleScope.launch {
                        eventsDao.deleteCrossRefsForEvent(event.id)
                        eventsDao.delete(event)

                        Toast.makeText(
                            this@ProfileActivity,
                            "Мероприятие удалено",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .show()
        }

        eventsRecycler.layoutManager =
            LinearLayoutManager(this)

        eventsRecycler.adapter = adapter

        lifecycleScope.launch {
            eventsDao.observeCreatedByUser(login).collect { events ->

                adapter.submitList(events)

                createdCount.text = events.size.toString()

                emptyEventsText.visibility =
                    if (events.isEmpty()) View.VISIBLE
                    else View.GONE

                eventsRecycler.visibility =
                    if (events.isEmpty()) View.GONE
                    else View.VISIBLE
            }
        }

        val avatarField = findViewById<ImageView>(R.id.profileAvatar)
        val nameText = findViewById<TextView>(R.id.profileName)
        val loginText = findViewById<TextView>(R.id.profileLogin)
        val emailText = findViewById<TextView>(R.id.profileEmail)

        if (login == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnLogout = findViewById<TextView>(R.id.logoutButton)
        btnLogout.setOnClickListener {
            session.clear()

            val intent = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
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