package com.example.unitime

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.db.events.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(private val onEventClick: (Event) -> Unit) : ListAdapter<Event, EventAdapter.EventViewHolder>(Diff) {
    private val dateFormatter = SimpleDateFormat(
        "EEEE, dd MMMM, HH:mm",
        Locale.getDefault()
    )

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById<TextView>(R.id.activityTitle)
        val date: TextView = view.findViewById<TextView>(R.id.activityDate)
        val location: TextView = view.findViewById<TextView>(R.id.activityLocation)
        val tag: TextView = view.findViewById<TextView>(R.id.activityCategory)
        val participants: TextView = view.findViewById<TextView>(R.id.activityParticipants)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)

        return EventViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int
    ) {
        val event = getItem(position)

        holder.title.text = event.activityTitle
        holder.date.text = dateFormatter.format(Date(event.activityDate))
        holder.location.text = event.activityLocation
        holder.tag.text = event.activityCategory
        holder.participants.text = "Допустимое количество участников: ${event.activityParticipants}"

        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    object Diff : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(a: Event, b: Event) = a.id == b.id
        override fun areContentsTheSame(a: Event, b: Event) = a == b
    }

}