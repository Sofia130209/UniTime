package com.example.unitime.ui.events

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.R
import com.example.unitime.data.local.model.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onJoinClick: (Event) -> Unit,
    private val onDeleteClick: ((Event) -> Unit)? = null
) : ListAdapter<Event, EventAdapter.EventViewHolder>(Diff) {

    private val dateFormatter = SimpleDateFormat("EEEE, dd MMMM, HH:mm", Locale.getDefault())

    // Храним состояния локально
    private val participantCounts = mutableMapOf<Long, Int>()
    private val joinedEvents = mutableSetOf<Long>()

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.activityTitle)
        val date: TextView = view.findViewById(R.id.activityDate)
        val location: TextView = view.findViewById(R.id.activityLocation)
        val tag: TextView = view.findViewById(R.id.activityCategory)
        val participants: TextView = view.findViewById(R.id.activityParticipants)
        val joinButton: ImageView = view.findViewById(R.id.joinButton)
        val deleteButton: ImageView? = view.findViewById(R.id.deleteEventButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        return EventViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)

        holder.title.text = event.activityTitle ?: "Без названия"
        holder.date.text = dateFormatter.format(Date(event.activityDate))
        holder.location.text = event.activityLocation ?: "Не указано"
        holder.tag.text = event.activityCategory ?: "Другое"

        // Берем актуальный счетчик из карты
        val count = participantCounts[event.id] ?: 0
        holder.participants.text = "$count / ${event.activityParticipants}"

        val isJoined = joinedEvents.contains(event.id)
        holder.joinButton.setBackgroundResource(if (isJoined) R.drawable.del else R.drawable.arrow_round)
        holder.joinButton.isEnabled = true
        holder.joinButton.isClickable = true

        holder.joinButton.setOnClickListener { onJoinClick(event) }
        holder.itemView.setOnClickListener { onEventClick(event) }
        holder.deleteButton?.setOnClickListener { onDeleteClick?.invoke(event) }
    }

    // === ЭТОТ МЕТОД ВЫЗЫВАЕТСЯ ИЗ FLOW И МГНОВЕННО ОБНОВЛЯЕТ КАРТОЧКУ ===
    @SuppressLint("NotifyDataSetChanged")
    fun updateParticipantCount(eventId: Long, count: Int) {
        if (participantCounts[eventId] != count) {
            participantCounts[eventId] = count
            val position = currentList.indexOfFirst { it.id == eventId }
            if (position != -1) {
                notifyItemChanged(position) // <-- Перерисовывает ТОЛЬКО эту карточку
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun markJoined(eventId: Long) {
        joinedEvents.add(eventId)
        val position = currentList.indexOfFirst { it.id == eventId }
        if (position != -1) notifyItemChanged(position)
    }

    fun isJoined(eventId: Long): Boolean = joinedEvents.contains(eventId)

    @SuppressLint("NotifyDataSetChanged")
    fun markNotJoined(eventId: Long) {
        joinedEvents.remove(eventId)
        val position = currentList.indexOfFirst { it.id == eventId }
        if (position != -1) notifyItemChanged(position)
    }

    object Diff : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(a: Event, b: Event) = a.id == b.id
        override fun areContentsTheSame(a: Event, b: Event) = a == b
    }
}