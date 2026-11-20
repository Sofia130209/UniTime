package com.example.unitime.ui.events

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.R
import com.example.unitime.data.local.model.Event
import com.example.unitime.data.local.model.User
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.remote.RetrofitClient
import com.example.unitime.ui.auth.ProfileActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailsActivity : AppCompatActivity() {
    private var isJoined = false
    private var eventId: Long = -1L
    private var currentUserLogin: String? = null
    private lateinit var joinButton: Button
    private lateinit var participantsText: TextView
    private lateinit var rvParticipants: RecyclerView
    private lateinit var participantAdapter: ParticipantAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        // 1. Получаем данные из Intent
        eventId = intent.getLongExtra(EXTRA_ID, -1L)
        if (eventId == -1L) {
            finish()
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: ""
        val date = intent.getLongExtra(EXTRA_DATE, 0L)
        val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
        val limit = intent.getIntExtra(EXTRA_PARTICIPANTS, 0)

        // 2. Находим View элементы и заполняем их
        findViewById<TextView>(R.id.detailTitle).text = title
        findViewById<TextView>(R.id.detailDescription).text = if (description.isBlank()) "Описание не добавлено." else description
        findViewById<TextView>(R.id.detailCategory).text = category

        val dateFormatter = SimpleDateFormat("EEEE, dd MMMM, HH:mm", Locale.getDefault())
        findViewById<TextView>(R.id.detailDate).text = dateFormatter.format(Date(date))
        findViewById<TextView>(R.id.detailLocation).text = location

        participantsText = findViewById(R.id.detailParticipants)
        joinButton = findViewById(R.id.detailJoinButton)
        findViewById<TextView>(R.id.backButton).setOnClickListener { finish() }

        // 3. Настраиваем RecyclerView для участников
        rvParticipants = findViewById(R.id.rvParticipantsList)
        rvParticipants.layoutManager = LinearLayoutManager(this)

        // 4. Получаем логин текущего пользователя
        currentUserLogin = SessionManager(this).userLogin

        // 5. Загружаем участников с сервера
        loadParticipantsFromServer(limit)

        joinButton.setOnClickListener {
            val login = currentUserLogin
            if (login == null) {
                Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            joinButton.isEnabled = false
            joinButton.text = "Загрузка..."

            lifecycleScope.launch {
                try {
                    if (!isJoined) {
                        // ИСПРАВЛЕНО: используем joinEventOnServer
                        val response = RetrofitClient.api.joinEventOnServer(eventId, login)
                        if (response.isSuccessful && response.body()?.success == true) {
                            isJoined = true
                            Toast.makeText(this@EventDetailsActivity, "Вы записались!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@EventDetailsActivity, "Не удалось записаться", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // ИСПРАВЛЕНО: используем leaveEventOnServer
                        val response = RetrofitClient.api.leaveEventOnServer(eventId, login)
                        if (response.isSuccessful && response.body()?.success == true) {
                            isJoined = false
                            Toast.makeText(this@EventDetailsActivity, "Запись отменена", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@EventDetailsActivity, "Не удалось отменить запись", Toast.LENGTH_SHORT).show()
                        }
                    }
                    // Перезагружаем список участников и обновляем кнопку
                    loadParticipantsFromServer(limit)
                } catch (e: Exception) {
                    Toast.makeText(this@EventDetailsActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    joinButton.isEnabled = true
                    updateJoinButtonText()
                }
            }
        }
    }

    private fun loadParticipantsFromServer(limit: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getEventParticipants(eventId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val serverParticipants = response.body()!!.participants
                    val localUsers = serverParticipants.map { dto ->
                        User(
                            login = dto.login ?: "",
                            name = dto.name ?: "",
                            surname = dto.surname ?: "",
                            email = dto.email ?: "",
                            password = "",
                            avatarPath = dto.avatarUrl
                        )
                    }

                    // Проверяем, записан ли текущий пользователь
                    isJoined = localUsers.any { it.login == currentUserLogin }
                    updateJoinButtonText()

                    // Устанавливаем адаптер
                    participantAdapter = ParticipantAdapter(localUsers) { selectedUser ->
                        val intent = Intent(this@EventDetailsActivity, ProfileActivity::class.java).apply {
                            putExtra(ProfileActivity.EXTRA_USER_LOGIN, selectedUser.login)
                        }
                        startActivity(intent)
                    }
                    rvParticipants.adapter = participantAdapter

                    // Обновляем счетчик
                    participantsText.text = "Участники: ${localUsers.size}/$limit"
                } else {
                    participantsText.text = "Не удалось загрузить участников"
                }
            } catch (e: Exception) {
                participantsText.text = "Ошибка сети"
                e.printStackTrace()
            }
        }
    }

    private fun updateJoinButtonText() {
        joinButton.text = if (isJoined) "Отменить запись" else "Присоединиться"
    }

    companion object {
        private const val EXTRA_ID = "event_id"
        private const val EXTRA_TITLE = "event_title"
        private const val EXTRA_DESCRIPTION = "event_description"
        private const val EXTRA_CATEGORY = "event_category"
        private const val EXTRA_DATE = "event_date"
        private const val EXTRA_LOCATION = "event_location"
        private const val EXTRA_PARTICIPANTS = "event_participants"

        fun newIntent(context: Context, event: Event): Intent {
            return Intent(context, EventDetailsActivity::class.java).apply {
                putExtra(EXTRA_ID, event.id)
                putExtra(EXTRA_TITLE, event.activityTitle)
                putExtra(EXTRA_DESCRIPTION, event.activityDescription)
                putExtra(EXTRA_CATEGORY, event.activityCategory)
                putExtra(EXTRA_DATE, event.activityDate)
                putExtra(EXTRA_LOCATION, event.activityLocation)
                putExtra(EXTRA_PARTICIPANTS, event.activityParticipants)
            }
        }
    }
}