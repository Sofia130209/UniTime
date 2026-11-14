
package com.example.unitime

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.utils.SessionManager
import com.example.unitime.db.events.EventRepository
import com.example.unitime.db.events.EventsDbProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.View
import android.widget.Toast
import com.example.unitime.utils.toEvents

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

        val createEvent = findViewById<Button>(R.id.createActivityButton)

        createEvent.setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        val recycler = findViewById<RecyclerView>(R.id.activitiesRecyclerView)

        val adapter = EventAdapter { event ->
            // TODO: открыть экран деталей события
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        val db = EventsDbProvider.get(this)

        val repository = EventRepository(
            db.getEventsDao(),
            db.getTagDao()
        )

        val emptyView = findViewById<View>(R.id.emptyView)

        lifecycleScope.launch {
            db.getEventsDao().observeUpcoming()
                .collect { events ->
                    adapter.submitList(events.toEvents())

                    emptyView.visibility =
                        if (events.isEmpty()) View.VISIBLE else View.GONE
                }
        }
    }
}
