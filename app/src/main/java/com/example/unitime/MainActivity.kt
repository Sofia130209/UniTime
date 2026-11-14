package com.example.unitime

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.db.events.EventRepository
import com.example.unitime.db.events.EventsDbProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        val createEvent = findViewById<Button>(R.id.createActivityButton)
        createEvent.setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        val recycler = findViewById<RecyclerView>(R.id.activitiesRecyclerView)
        val emptyView = findViewById<View>(R.id.emptyView)

        val adapter = EventAdapter { event ->
            // TODO: открыть экран деталей
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val db = EventsDbProvider.get(this)
        val repository = EventRepository(db.getEventsDao(), db.getTagDao())
        lifecycleScope.launch {
            // * Repository возвращает уже готовый Flow<List<Event>> — никаких кастов
            repository.observeUpcoming()
                .collect { events ->
                    adapter.submitList(events)
                    emptyView.visibility =
                        if (events.isEmpty()) View.VISIBLE else View.GONE
                }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topPanel)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top + v.paddingTop, v.paddingRight, v.paddingBottom)
            insets
        }
    }
}