package com.example.unitime.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.local.db.events.EventsDbProvider
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.repo.EventParticipantRepository
import com.example.unitime.ui.BottomNavigationHelper
import com.example.unitime.ui.events.ProfileEventsAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProfileActivity : ComponentActivity() {

    private lateinit var session: SessionManager

    private lateinit var avatarField: ImageView
    private lateinit var nameText: TextView
    private lateinit var loginText: TextView
    private lateinit var emailText: TextView

    // НОВЫЕ ПЕРЕМЕННЫЕ для контактов
    private lateinit var telegramText: TextView
    private lateinit var phoneText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. СНАЧАЛА устанавливаем разметку
        setContentView(R.layout.activity_profile)

        // 2. Инициализируем сессию
        session = SessionManager(this)
        val login = session.userLogin

        if (login == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 3. Находим все View
        avatarField = findViewById(R.id.profileAvatar)
        nameText = findViewById(R.id.profileName)
        loginText = findViewById(R.id.profileLogin)
        emailText = findViewById(R.id.profileEmail)

        // Инициализация полей контактов
        telegramText = findViewById(R.id.profileTelegram)
        phoneText = findViewById(R.id.profilePhone)

        val tvJoinedCount = findViewById<TextView>(R.id.profileJoinedCount)
        val tvCreatedCount = findViewById<TextView>(R.id.profileCreatedCount)

        // 4. Настраиваем навигацию
        BottomNavigationHelper.setup(this, R.id.nav_profile)

        // 5. Настраиваем базу данных и репозитории
        val db = EventsDbProvider.get(this)
        val usersDb = UsersDbProvider.get(this)
        val participantRepository = EventParticipantRepository(
            db.getEventParticipantDao(),
            usersDb.getUsersDao()
        )

        // 6. Подписываемся на обновление счетчиков
        lifecycleScope.launch {
            // Счетчик событий, на которые записан пользователь
            participantRepository.observeJoinedEventsCount(login).collect { count ->
                tvJoinedCount.text = count.toString()
            }
        }

        lifecycleScope.launch {
            // Счетчик созданных событий
            db.getEventsDao().observeCreatedCount(login).collect { count ->
                tvCreatedCount.text = count.toString()
            }
        }

        // 7. Загружаем остальные данные
        loadUserData()
        setupEvents(login)
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        val nav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        nav.post {
            BottomNavigationHelper.setup(this, R.id.nav_profile)
        }
        loadUserData()
    }

    // Загрузка актуальной информации о пользователе
    private fun loadUserData() {
        val login = session.userLogin ?: return

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                UsersDbProvider.get(applicationContext)
                    .getUsersDao()
                    .getUserByLogin(login)
            }

            if (user != null) {
                nameText.text = "${user.name} ${user.surname}"
                loginText.text = "@${user.login}"
                emailText.text = user.email

                // Отображение контактов (если пусто - показываем "Не указан")
                telegramText.text = if (user.telegram.isNullOrBlank()) {
                    "Не указан"
                } else {
                    user.telegram
                }

                phoneText.text = if (user.phone.isNullOrBlank()) {
                    "Не указан"
                } else {
                    user.phone
                }

                val avatarFile = user.avatarPath
                    ?.let(::File)
                    ?.takeIf { it.exists() }

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
                Toast.makeText(
                    this@ProfileActivity,
                    "Пользователь не найден",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Настройка мероприятий пользователя
    private fun setupEvents(login: String) {
        val eventsRecycler = findViewById<RecyclerView>(R.id.profileEventsRecycler)
        val emptyEventsText = findViewById<TextView>(R.id.emptyEventsText)

        val eventsDao = EventsDbProvider.get(this).getEventsDao()

        val adapter = ProfileEventsAdapter { event ->
            AlertDialog.Builder(this)
                .setTitle("Удалить мероприятие?")
                .setMessage(
                    "Вы действительно хотите удалить «${event.activityTitle}»? " +
                            "Это действие нельзя отменить."
                )
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            eventsDao.deleteCrossRefsForEvent(event.id)
                            eventsDao.delete(event)
                        }
                        Toast.makeText(
                            this@ProfileActivity,
                            "Мероприятие удалено",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .show()
        }

        eventsRecycler.layoutManager = LinearLayoutManager(this)
        eventsRecycler.adapter = adapter

        lifecycleScope.launch {
            eventsDao.observeCreatedByUser(login).collect { events ->
                adapter.submitList(events)

                // Счетчик теперь обновляется через Flow в onCreate,
                // поэтому ручное обновление здесь не нужно.

                emptyEventsText.visibility =
                    if (events.isEmpty()) View.VISIBLE else View.GONE

                eventsRecycler.visibility =
                    if (events.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    // Настройка кнопок профиля
    private fun setupButtons() {
        val btnEditProfile = findViewById<View>(R.id.editProfileButton)

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        val btnLogout = findViewById<TextView>(R.id.logoutButton)
        btnLogout.setOnClickListener {
            session.clear()

            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        }
    }
}