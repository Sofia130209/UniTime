package com.example.unitime.ui.events

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.local.db.events.EventsDbProvider
import com.example.unitime.data.local.model.Event
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.repo.EventParticipantRepository
import com.example.unitime.data.repo.JoinResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailsActivity : AppCompatActivity() {

    private var isJoined = false
    private var eventId: Long = -1L

    private lateinit var participantRepository: EventParticipantRepository
    private var userLogin: String? = null

    private lateinit var joinButton: Button
    private lateinit var participantsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        // Получаем данные мероприятия
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

        // Находим элементы интерфейса
        val titleText = findViewById<TextView>(R.id.detailTitle)
        val descriptionText = findViewById<TextView>(R.id.detailDescription)
        val categoryText = findViewById<TextView>(R.id.detailCategory)
        val dateText = findViewById<TextView>(R.id.detailDate)
        val locationText = findViewById<TextView>(R.id.detailLocation)

        participantsText = findViewById(R.id.detailParticipants)
        joinButton = findViewById(R.id.detailJoinButton)

        val backButton = findViewById<TextView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Заполняем экран
        titleText.text = title
        descriptionText.text =
            if (description.isBlank()) "Описание не добавлено." else description
        categoryText.text = category

        val dateFormatter = SimpleDateFormat(
            "EEEE, dd MMMM, HH:mm",
            Locale.getDefault()
        )
        dateText.text = dateFormatter.format(Date(date))

        locationText.text = location

        // Подключаем базы данных
        val session = SessionManager(this)
        userLogin = session.userLogin

        val eventsDb = EventsDbProvider.get(this)
        val usersDb = UsersDbProvider.get(this)

        participantRepository = EventParticipantRepository(
            eventsDb.getEventParticipantDao(),
            usersDb.getUsersDao()
        )

        // Следим за количеством участников
        lifecycleScope.launch {
            participantRepository.observeParticipantCount(eventId).collect { count ->
                participantsText.text = "$count/$limit человека"
            }
        }

        // Проверяем, записан ли пользователь
        lifecycleScope.launch {
            val login = userLogin ?: return@launch

            isJoined = participantRepository.hasJoined(eventId, login)
            updateJoinButton()
        }

        // Запись или отмена записи
        joinButton.setOnClickListener {
            lifecycleScope.launch {
                val login = userLogin

                if (login == null) {
                    Toast.makeText(
                        this@EventDetailsActivity,
                        "Не удалось определить пользователя",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (isJoined) {
                    participantRepository.leaveEvent(eventId, login)
                    isJoined = false
                    updateJoinButton()

                    Toast.makeText(
                        this@EventDetailsActivity,
                        "Вы отменили запись",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    when (participantRepository.joinEvent(eventId, login)) {

                        JoinResult.SUCCESS -> {
                            isJoined = true
                            updateJoinButton()

                            Toast.makeText(
                                this@EventDetailsActivity,
                                "Вы записались на мероприятие!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        JoinResult.ALREADY_JOINED -> {
                            isJoined = true
                            updateJoinButton()
                        }

                        JoinResult.FULL -> {
                            Toast.makeText(
                                this@EventDetailsActivity,
                                "Свободных мест больше нет",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        JoinResult.USER_NOT_FOUND -> {
                            Toast.makeText(
                                this@EventDetailsActivity,
                                "Пользователь не найден",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateJoinButton() {
        joinButton.text =
            if (isJoined) "Отменить запись" else "Присоединиться"
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
