package com.example.unitime.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.example.unitime.data.repo.EventParticipantRepository
import com.example.unitime.data.repo.EventRepository
import com.example.unitime.data.repo.JoinResult
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

        BottomNavigationHelper.setup(this, R.id.nav_home)
        findViewById<Button>(R.id.createActivityButton).setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }
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
        val participantJobs = mutableListOf<Job>()

        fun observeParticipantStates(events: List<Event>) {
            participantJobs.forEach { it.cancel() }
            participantJobs.clear()

            events.forEach { event ->
                participantJobs.add(
                    lifecycleScope.launch {
                        participantRepository
                            .observeParticipantCount(event.id)
                            .collect { count ->
                                adapter.updateParticipantCount(event.id, count)
                            }
                    }
                )
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
                repository.observeUpcoming().collect { events ->
                    adapter.submitList(events) {
                        observeParticipantStates(events)
                    }

                    emptyView.visibility =
                        if (events.isEmpty()) View.VISIBLE else View.GONE
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

                    emptyView.visibility =
                        if (events.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        filterByTagHandler = ::filterByTag
        showAllEventsHandler = ::showAllEvents
        findViewById<View>(R.id.categorySport).setOnClickListener {
            filterByTag("спорт")
        }

        findViewById<View>(R.id.categoryMeetings).setOnClickListener {
            filterByTag("встречи")
        }

        findViewById<View>(R.id.categoryStudy).setOnClickListener {
            filterByTag("учёба")
        }

        findViewById<View>(R.id.categoryOther).setOnClickListener {
            filterByTag("другое")
        }
        findViewById<View>(R.id.categoriesAll).setOnClickListener {
            categoryLauncher.launch(
                Intent(this, CategoriesActivity::class.java)
            )
        }
        showAllEvents()
    }

    override fun onResume() {
        super.onResume()
        val nav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        nav.post {
            BottomNavigationHelper.setup(this, R.id.nav_home)
        }
    }
}