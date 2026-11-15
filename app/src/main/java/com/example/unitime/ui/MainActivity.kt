package com.example.unitime.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.R
import com.example.unitime.data.local.model.Event
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.local.db.events.EventsDbProvider
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.repo.EventParticipantRepository
import com.example.unitime.data.repo.EventRepository
import com.example.unitime.data.repo.JoinResult
import com.example.unitime.ui.auth.LoginActivity
import com.example.unitime.ui.auth.ProfileActivity
import com.example.unitime.ui.events.AddEventActivity
import com.example.unitime.ui.events.EventAdapter
import com.example.unitime.ui.events.EventDetailsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val session = SessionManager(this)

        if (!session.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        BottomNavigationHelper.setup(this, R.id.nav_home)

        // Кнопка создания события
        findViewById<Button>(R.id.createActivityButton).setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        // База данных и репозитории
        val db = EventsDbProvider.get(this)

        val repository = EventRepository(
            db.getEventsDao(),
            db.getTagDao()
        )

        val usersDb = UsersDbProvider.get(this)

        val participantRepository = EventParticipantRepository(
            db.getEventParticipantDao(),
            usersDb.getUsersDao()
        )

        val userLogin = session.userLogin

        // RecyclerView
        val recycler = findViewById<RecyclerView>(R.id.activitiesRecyclerView)

        lateinit var adapter: EventAdapter
        adapter = EventAdapter(
            onEventClick = { event ->
                val intent = EventDetailsActivity.newIntent(this, event)
                startActivity(intent)
            },
            onJoinClick = { event ->
                lifecycleScope.launch {
                    val login = userLogin

                    if (login == null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Не удалось определить пользователя",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    if (adapter.isJoined(event.id)) {

                        // Отмена записи
                        participantRepository.leaveEvent(event.id, login)
                        adapter.markNotJoined(event.id)

                        Toast.makeText(
                            this@MainActivity,
                            "Вы отменили запись",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        // Запись на мероприятие
                        when (participantRepository.joinEvent(event.id, login)) {

                            JoinResult.SUCCESS -> {
                                adapter.markJoined(event.id)

                                Toast.makeText(
                                    this@MainActivity,
                                    "Вы записались на мероприятие!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            JoinResult.ALREADY_JOINED -> {
                                adapter.markJoined(event.id)

                                Toast.makeText(
                                    this@MainActivity,
                                    "Вы уже записаны",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            JoinResult.FULL -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Свободных мест больше нет",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            JoinResult.USER_NOT_FOUND -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Пользователь не найден",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Подписки на изменения участников
        val participantJobs = mutableListOf<Job>()

        fun observeParticipantStates(events: List<Event>) {
            participantJobs.forEach { it.cancel() }
            participantJobs.clear()

            events.forEach { event ->

                // Количество участников
                participantJobs.add(
                    lifecycleScope.launch {
                        participantRepository
                            .observeParticipantCount(event.id)
                            .collect { count ->
                                adapter.updateParticipantCount(event.id, count)
                            }
                    }
                )

                // Проверка записи текущего пользователя
                participantJobs.add(
                    lifecycleScope.launch {
                        val login = userLogin ?: return@launch

                        if (participantRepository.hasJoined(event.id, login)) {
                            adapter.markJoined(event.id)
                        }
                    }
                )
            }
        }

        lifecycleScope.launch {
            repository.ensureDefaultTags()
        }

        val emptyView = findViewById<View>(R.id.emptyView)

        // Управление подпиской на события
        var eventsJob: Job? = null

        // Отображение всех предстоящих событий
        fun showAllEvents() {
            eventsJob?.cancel()

            eventsJob = lifecycleScope.launch {
                repository.observeUpcoming().collect { events ->
                    adapter.submitList(events) {
                        observeParticipantStates(events)
                    }

                    emptyView.visibility =
                        if (events.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // Фильтрация по тегу
        fun filterByTag(tagName: String) {
            eventsJob?.cancel()

            eventsJob = lifecycleScope.launch {
                val tagId = repository.findTagIdByName(tagName)

                if (tagId == null) {
                    adapter.submitList(emptyList()) {
                        observeParticipantStates(emptyList())
                    }
                    emptyView.visibility = View.VISIBLE
                    return@launch
                }

                repository.observeByTag(tagId).collect { events ->
                    adapter.submitList(events) {
                        observeParticipantStates(events)
                    }

                    emptyView.visibility =
                        if (events.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // Кнопки фильтрации
        findViewById<Button>(R.id.categoryAll).setOnClickListener {
            showAllEvents()
        }

        findViewById<Button>(R.id.categorySport).setOnClickListener {
            filterByTag("спорт")
        }

        findViewById<Button>(R.id.categoryMusic).setOnClickListener {
            filterByTag("музыка")
        }

        findViewById<Button>(R.id.categoryStudy).setOnClickListener {
            filterByTag("учёба")
        }

        findViewById<Button>(R.id.categoryFood).setOnClickListener {
            filterByTag("еда")
        }

        findViewById<Button>(R.id.categoryGaming).setOnClickListener {
            filterByTag("игры")
        }

        findViewById<Button>(R.id.categoryArt).setOnClickListener {
            filterByTag("искусство")
        }

        findViewById<Button>(R.id.categoryVolunteering).setOnClickListener {
            filterByTag("волонтёрство")
        }

        findViewById<Button>(R.id.categoryOther).setOnClickListener {
            filterByTag("другое")
        }

        // Первоначальная загрузка
        showAllEvents()


    }
}
