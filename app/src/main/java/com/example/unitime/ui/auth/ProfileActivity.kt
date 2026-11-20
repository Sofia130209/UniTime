package com.example.unitime.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.local.db.events.EventsDbProvider
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.remote.RetrofitClient
import com.example.unitime.data.repo.EventParticipantRepository
import com.example.unitime.ui.BottomNavigationHelper
import com.example.unitime.ui.events.ProfileEventsAdapter
import com.example.unitime.ui.showDeleteConfirmationDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProfileActivity : ComponentActivity() {

    private lateinit var session: SessionManager
    private var targetLogin: String? = null

    private lateinit var avatarField: ImageView
    private lateinit var nameText: TextView
    private lateinit var loginText: TextView
    private lateinit var emailText: TextView
    private lateinit var telegramText: TextView
    private lateinit var phoneText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        session = SessionManager(this)
        targetLogin = intent.getStringExtra(EXTRA_USER_LOGIN) ?: session.userLogin
        val login = targetLogin

        if (login == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        avatarField = findViewById(R.id.profileAvatar)
        nameText = findViewById(R.id.profileName)
        loginText = findViewById(R.id.profileLogin)
        emailText = findViewById(R.id.profileEmail)
        telegramText = findViewById(R.id.profileTelegram)
        phoneText = findViewById(R.id.profilePhone)

        val tvJoinedCount = findViewById<TextView>(R.id.profileJoinedCount)
        val tvCreatedCount = findViewById<TextView>(R.id.profileCreatedCount)

        BottomNavigationHelper.setup(this, R.id.nav_profile)

        val db = EventsDbProvider.get(this)
        val usersDb = UsersDbProvider.get(this)
        val participantRepository = EventParticipantRepository(
            db.getEventParticipantDao(),
            usersDb.getUsersDao()
        )

        lifecycleScope.launch {
            participantRepository.observeJoinedEventsCount(login).collect { count ->
                tvJoinedCount.text = count.toString()
            }
        }

        lifecycleScope.launch {
            db.getEventsDao().observeCreatedCount(login).collect { count ->
                tvCreatedCount.text = count.toString()
            }
        }

        loadUserData(login)
        setupEvents(login)
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        val nav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        nav.post {
            BottomNavigationHelper.setup(this, R.id.nav_profile)
        }
        targetLogin?.let { loadUserData(it) }
    }

    // === ИСПРАВЛЕННЫЙ МЕТОД ЗАГРУЗКИ ===
    private fun loadUserData(login: String) {
        lifecycleScope.launch {
            try {
                // 1. Пытаемся получить данные с сервера (для чужих профилей)
                val response = RetrofitClient.api.getUserByLogin(login)

                if (response.isSuccessful && response.body()?.success == true) {
                    val userDto = response.body()?.user
                    if (userDto != null) {
                        updateUIFromDto(userDto)
                        return@launch // Успешно загрузили, выходим
                    }
                }

                // 2. Если с сервера не вышло, ищем в локальной БД (фоллбэк для своего профиля без интернета)
                val localUser = withContext(Dispatchers.IO) {
                    UsersDbProvider.get(applicationContext).getUsersDao().getUserByLogin(login)
                }

                if (localUser != null) {
                    updateUIFromLocal(localUser)
                } else {
                    Toast.makeText(this@ProfileActivity, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // При ошибке сети тоже пробуем локальную БД
                val localUser = withContext(Dispatchers.IO) {
                    UsersDbProvider.get(applicationContext).getUsersDao().getUserByLogin(login)
                }
                if (localUser != null) {
                    updateUIFromLocal(localUser)
                } else {
                    Toast.makeText(this@ProfileActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUIFromDto(user: com.example.unitime.data.remote.UserDto) {
        nameText.text = "${user.name} ${user.surname}".trim().ifBlank { "Пользователь" }
        loginText.text = "@${user.login}"
        emailText.text = user.email ?: "Не указан"
        telegramText.text = if (user.telegram.isNullOrBlank()) "Не указан" else user.telegram
        phoneText.text = if (user.phone.isNullOrBlank()) "Не указан" else user.phone

        if (!user.avatarUrl.isNullOrBlank()) {
            val fullUrl = if (user.avatarUrl.startsWith("http")) user.avatarUrl else "http://87.232.69.164/${user.avatarUrl}"
            avatarField.load(fullUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder)
                transformations(CircleCropTransformation())
            }
        } else {
            avatarField.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun updateUIFromLocal(user: com.example.unitime.data.local.model.User) {
        nameText.text = "${user.name} ${user.surname}".trim()
        loginText.text = "@${user.login}"
        emailText.text = user.email ?: "Не указан"
        telegramText.text = if (user.telegram.isNullOrBlank()) "Не указан" else user.telegram
        phoneText.text = if (user.phone.isNullOrBlank()) "Не указан" else user.phone

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
    }

    private fun setupEvents(login: String) {
        val eventsRecycler = findViewById<RecyclerView>(R.id.profileEventsRecycler)
        val emptyEventsText = findViewById<TextView>(R.id.emptyEventsText)
        val eventsDao = EventsDbProvider.get(this).getEventsDao()

        val adapter = ProfileEventsAdapter { event ->
            showDeleteConfirmationDialog(
                context = this,
                title = "Удалить мероприятие?",
                message = "Вы действительно хотите удалить «${event.activityTitle}»?\nЭто действие нельзя отменить."
            ) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        eventsDao.deleteCrossRefsForEvent(event.id)
                        eventsDao.delete(event)
                    }
                    Toast.makeText(this@ProfileActivity, "Мероприятие удалено", Toast.LENGTH_SHORT).show()
                }
            }
        }

        eventsRecycler.layoutManager = LinearLayoutManager(this)
        eventsRecycler.adapter = adapter

        lifecycleScope.launch {
            eventsDao.observeCreatedByUser(login).collect { events ->
                adapter.submitList(events)
                emptyEventsText.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                eventsRecycler.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun setupButtons() {
        val btnEditProfile = findViewById<View>(R.id.editProfileButton)
        val logoutCard = findViewById<View>(R.id.logoutCard)
        val blocktext = findViewById<TextView>(R.id.emptyEventsText)

        if (targetLogin == session.userLogin) {
            btnEditProfile.visibility = View.VISIBLE
            logoutCard.visibility = View.VISIBLE

            btnEditProfile.setOnClickListener {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }

            logoutCard.setOnClickListener {
                session.clear()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        } else {
            btnEditProfile.visibility = View.GONE
            logoutCard.visibility = View.GONE
            blocktext.visibility = View.GONE
        }
    }

    companion object {
        const val EXTRA_USER_LOGIN = "extra_user_login"
    }
}