package com.example.unitime.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.R
import com.example.unitime.data.local.db.auth.UsersDbProvider
import com.example.unitime.data.local.db.events.EventsDbProvider
import com.example.unitime.data.local.model.Event
import com.example.unitime.data.managers.SessionManager
import com.example.unitime.data.remote.RetrofitClient
import com.example.unitime.data.repo.EventParticipantRepository
import com.example.unitime.data.repo.EventRepository
import com.example.unitime.ui.auth.LoginActivity
import com.example.unitime.ui.categories.CategoriesActivity
import com.example.unitime.ui.events.AddEventActivity
import com.example.unitime.ui.events.EventAdapter
import com.example.unitime.ui.events.EventDetailsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var filterByTagHandler: ((String) -> Unit)? = null
    private var showAllEventsHandler: (() -> Unit)? = null

    private val categoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val tag = result.data?.getStringExtra("tagName")
            if (tag.isNullOrBlank()) {
                showAllEventsHandler?.invoke()
            } else {
                filterByTagHandler?.invoke(tag)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)
        if (!session.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvCreatedCount = findViewById<TextView>(R.id.tvCreatedCount)
        val tvJoinedCount = findViewById<TextView>(R.id.tvJoinedCount)

        BottomNavigationHelper.setup(this, R.id.nav_home)
        findViewById<Button>(R.id.createActivityButton).setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        val db = EventsDbProvider.get(this)
        val repository = EventRepository(db.getEventsDao(), db.getTagDao())
        val usersDb = UsersDbProvider.get(this)
        val participantRepository = EventParticipantRepository(
            db.getEventParticipantDao(),
            usersDb.getUsersDao()
        )

        val userLogin = session.userLogin

        if (userLogin != null) {
            lifecycleScope.launch {
                db.getEventsDao().observeCreatedCount(userLogin).collect { count ->
                    tvCreatedCount.text = count.toString()
                }
            }
            lifecycleScope.launch {
                participantRepository.observeJoinedEventsCount(userLogin).collect { count ->
                    tvJoinedCount.text = count.toString()
                }
            }
        } else {
            tvCreatedCount.text = "0"
            tvJoinedCount.text = "0"
        }

        val recycler = findViewById<RecyclerView>(R.id.activitiesRecyclerView)
        lateinit var adapter: EventAdapter

        adapter = EventAdapter(
            onEventClick = { event ->
                val intent = EventDetailsActivity.newIntent(this, event)
                startActivity(intent)
            },
            onJoinClick = { event ->
                lifecycleScope.launch {
                    val login = userLogin ?: return@launch

                    if (adapter.isJoined(event.id)) {
                        // === ОТМЕНА ЗАПИСИ ===
                        try {
                            val response = RetrofitClient.api.leaveEventOnServer(event.id, login)
                            if (response.isSuccessful && response.body()?.success == true) {
                                // Удаляем из локальной БД — Flow мгновенно обновит счётчик на карточке
                                participantRepository.leaveEvent(event.id, login)
                                adapter.markNotJoined(event.id)
                                Toast.makeText(this@MainActivity, "Вы отменили запись", Toast.LENGTH_SHORT).show()

                                // ⚠️ ВАЖНО: showAllEventsHandler?.invoke() УБРАН!
                                // Иначе он перезатрёт мгновенное обновление из Room
                            } else {
                                Toast.makeText(this@MainActivity, "Не удалось отменить запись", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // === ЗАПИСЬ НА МЕРОПРИЯТИЕ ===
                        try {
                            val response = RetrofitClient.api.joinEventOnServer(event.id, login)
                            val body = response.body()
                            if (response.isSuccessful && body?.success == true) {
                                // Добавляем в локальную БД — Flow мгновенно обновит счётчик на карточке
                                participantRepository.joinEvent(event.id, login)
                                adapter.markJoined(event.id)
                                Toast.makeText(this@MainActivity, "Вы записались!", Toast.LENGTH_SHORT).show()

                                // ⚠️ ВАЖНО: showAllEventsHandler?.invoke() УБРАН!
                            } else {
                                val errorMsg = when (body?.error) {
                                    "Event is full" -> "Мест больше нет"
                                    "Already joined" -> "Вы уже записаны"
                                    else -> "Не удалось записаться"
                                }
                                Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDeleteClick = { event ->
                // === УДАЛЕНИЕ МЕРОПРИЯТИЯ ===
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.deleteEvent(event.id)
                        if (response.isSuccessful) {
                            val localDb = EventsDbProvider.get(this@MainActivity)
                            localDb.getEventsDao().getById(event.id)?.let {
                                localDb.getEventsDao().delete(it)
                            }
                            Toast.makeText(this@MainActivity, "Мероприятие удалено", Toast.LENGTH_SHORT).show()
                            // Здесь перезагрузка НУЖНА, чтобы карточка исчезла
                            showAllEventsHandler?.invoke()
                        } else {
                            Toast.makeText(this@MainActivity, "Не удалось удалить", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val participantJobs = mutableListOf<Job>()

        fun observeParticipantStates(events: List<Event>) {
            participantJobs.forEach { it.cancel() }
            participantJobs.clear()

            events.forEach { event ->
                // 1. Следим за количеством участников (Flow из Room)
                participantJobs.add(
                    lifecycleScope.launch {
                        participantRepository.observeParticipantCount(event.id).collect { count ->
                            adapter.updateParticipantCount(event.id, count)
                        }
                    }
                )
                // 2. Следим за статусом "записан ли я"
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
        var eventsJob: Job? = null

        fun showAllEvents() {
            eventsJob?.cancel()

            eventsJob = lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.getAllEvents()
                    println("🌐 Ответ сервера: isSuccess=${response.isSuccessful}, body=${response.body()?.size ?: 0} событий")

                    if (response.isSuccessful && response.body() != null) {
                        val cloudEvents = response.body()!!

                        val localEvents = cloudEvents.map { dto ->
                            Event(
                                id = dto.id,
                                activityTitle = dto.activityTitle ?: "Без названия",
                                activityDescription = dto.activityDescription ?: "",
                                activityCategory = dto.activityCategory ?: "Другое",
                                activityDate = dto.activityDate,
                                activityLocation = dto.activityLocation ?: "",
                                activityParticipants = dto.activityParticipants,
                                createdByLogin = dto.createdByLogin ?: ""
                            )
                        }

                        println("✅ Преобразовано в локальные события: ${localEvents.size}")

                        adapter.submitList(localEvents) {
                            observeParticipantStates(localEvents)
                        }
                        emptyView.visibility = if (localEvents.isEmpty()) View.VISIBLE else View.GONE

                    } else {
                        println("❌ Ошибка сервера, переключаемся на локальную БД")
                        repository.observeUpcoming().collect { events ->
                            adapter.submitList(events) {
                                observeParticipantStates(events)
                            }
                            emptyView.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Исключение сети: ${e.message}")
                    repository.observeUpcoming().collect { events ->
                        adapter.submitList(events) {
                            observeParticipantStates(events)
                        }
                        emptyView.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }

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
                    emptyView.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        filterByTagHandler = ::filterByTag
        showAllEventsHandler = ::showAllEvents

        findViewById<View>(R.id.categorySport).setOnClickListener { filterByTag("спорт") }
        findViewById<View>(R.id.categoryMeetings).setOnClickListener { filterByTag("встречи") }
        findViewById<View>(R.id.categoryStudy).setOnClickListener { filterByTag("учёба") }
        findViewById<View>(R.id.categoryOther).setOnClickListener { filterByTag("другое") }
        findViewById<View>(R.id.categoriesAll).setOnClickListener {
            categoryLauncher.launch(Intent(this, CategoriesActivity::class.java))
        }

        showAllEvents()
    }

    override fun onResume() {
        super.onResume()
        val nav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        nav.post {
            BottomNavigationHelper.setup(this, R.id.nav_home)
        }
        showAllEventsHandler?.invoke()
    }
}