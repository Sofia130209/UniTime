package com.example.unitime.data.repo

import com.example.unitime.data.local.db.tags.DefaultTags
import com.example.unitime.data.local.model.Tag
import com.example.unitime.data.local.model.Event
import com.example.unitime.data.local.db.events.EventsDao
import com.example.unitime.data.local.db.events.TagDao
import com.example.unitime.data.mapper.toEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(
    private val eventsDao: EventsDao,
    private val tagDao: TagDao
) {
    suspend fun ensureDefaultTags() {
        tagDao.insertAll(
            DefaultTags.ALL.map { name ->
                Tag(name = name)
            }
        )
    }
    fun observeUpcoming(
        now: Long = System.currentTimeMillis()
    ): Flow<List<Event>> =
        eventsDao.observeUpcoming(now).map { it.toEvents() }

    fun observeAll(): Flow<List<Event>> = eventsDao.observeAll().map { it.toEvents() }

    fun observeByTag(
        tagId: Long
    ): Flow<List<Event>> =
        eventsDao.observeByTag(tagId).map { it.toEvents() }

    suspend fun findTagIdByName(name: String): Long? =
        tagDao.findByName(name.trim().lowercase())?.id

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