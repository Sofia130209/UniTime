package com.example.unitime.db.events

import com.example.unitime.db.tags.Tag
import kotlinx.coroutines.flow.Flow

class EventRepository(
    private val eventsDao: EventsDao,
    private val tagDao: TagDao
) {
    fun observeUpcoming(): Flow<List<EventWithTags>> =
        eventsDao.observeUpcomingWithTags()

    fun observeByTag(tagId: Long): Flow<List<EventWithTags>> =
        eventsDao.observeByTag(tagId)

    fun observeAllTags(): Flow<List<Tag>> = tagDao.observeAll()

    suspend fun createEvent(event: Event, tagNames: List<String>): Long {
        val normalized = tagNames.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
        val ids = normalized.map { name ->
            tagDao.insert(Tag(name = name)).let { newId ->
                if (newId != -1L) newId else tagDao.findByName(name)!!.id
            }
        }
        return eventsDao.insertEventWithTags(event, ids)
    }
}