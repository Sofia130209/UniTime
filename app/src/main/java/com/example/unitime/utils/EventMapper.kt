package com.example.unitime.utils

import com.example.unitime.db.events.Event
import com.example.unitime.db.events.EventWithTags

// * Room возвращает из DAO список EventWithTags, а не "сырые" Event.
// * Без явного маппинга происходит ClassCastException и приложение падает при переходе на главный экран.
fun List<EventWithTags>.toEvents(): List<Event> = map { it.event }

fun EventWithTags?.toEventOrNull(): Event? = this?.event
