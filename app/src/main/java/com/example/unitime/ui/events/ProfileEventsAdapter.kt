package com.example.unitime.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.R
import com.example.unitime.data.local.model.Event
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileEventsAdapter(
    private val onDeleteClick: (Event) -> Unit
) : RecyclerView.Adapter<ProfileEventsAdapter.EventViewHolder>() {

    private var events = emptyList<Event>()

    class EventViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val title: TextView =
            view.findViewById(R.id.activityTitle)

        val category: TextView =
            view.findViewById(R.id.activityCategory)

        val date: TextView =
            view.findViewById(R.id.activityDate)

        val location: TextView =
            view.findViewById(R.id.activityLocation)

        val participants: TextView =
            view.findViewById(R.id.activityParticipants)

        val deleteButton: MaterialButton =
            view.findViewById(R.id.deleteEventButton)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)

        return EventViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int
    ) {
        val event = events[position]

        holder.title.text = event.activityTitle
        holder.category.text = event.activityCategory

        holder.date.text = SimpleDateFormat(
            "EEEE, dd MMMM, HH:mm",
            Locale.getDefault()
        ).format(Date(event.activityDate))

        holder.location.text = event.activityLocation

        holder.participants.text =
            "Необходимо ${event.activityParticipants} участников"

        holder.deleteButton.setOnClickListener {
            onDeleteClick(event)
        }
    }

    override fun getItemCount(): Int = events.size

    fun submitList(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
