package com.example.unitime.utils

import com.example.unitime.db.events.Event
import com.example.unitime.db.events.EventWithTags
fun List<EventWithTags>.toEvents(): List<Event> = map { it.event }

fun EventWithTags?.toEventOrNull(): Event? = this?.event
