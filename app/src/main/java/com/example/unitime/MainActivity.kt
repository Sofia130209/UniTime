package com.example.unitime

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.db.events.EventRepository
import com.example.unitime.db.events.EventsDbProvider
import com.example.unitime.utils.SessionManager
import com.example.unitime.utils.toEvents
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

        // Кнопка создания события
        findViewById<Button>(R.id.createActivityButton).setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        // RecyclerView
        val recycler = findViewById<RecyclerView>(R.id.activitiesRecyclerView)
        val adapter = EventAdapter { event ->
            // TODO: открыть экран деталей события
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // База данных и репозиторий
        val db = EventsDbProvider.get(this)

        val repository = EventRepository(
            db.getEventsDao(),
            db.getTagDao()
        )
        lifecycleScope.launch {
            repository.ensureDefaultTags()
        }

        val emptyView = findViewById<View>(R.id.emptyView)

        // Job для управления текущей подпиской на события
        var eventsJob: Job? = null

        // Отображение всех предстоящих событий
        fun showAllEvents() {
            eventsJob?.cancel()

            eventsJob = lifecycleScope.launch {
                repository.observeUpcoming().collect { events ->
                    adapter.submitList(events)

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

                Toast.makeText(
                    this@MainActivity,
                    "Тег: $tagName, ID: $tagId",
                    Toast.LENGTH_LONG
                ).show()

                if (tagId == null) {
                    adapter.submitList(emptyList())
                    emptyView.visibility = View.VISIBLE
                    return@launch
                }

                repository.observeByTag(tagId).collect { events ->
                    adapter.submitList(events)

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
