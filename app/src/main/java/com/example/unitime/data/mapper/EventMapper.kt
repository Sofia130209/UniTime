package com.example.unitime.data.mapper

import com.example.unitime.data.local.model.Event
import com.example.unitime.data.local.model.EventWithTags

fun List<EventWithTags>.toEvents(): List<Event> = map { it.event }

fun EventWithTags?.toEventOrNull(): Event? = this?.event
