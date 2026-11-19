package com.example.unitime.data.repo

import com.example.unitime.data.local.dao.EventParticipantDao
import com.example.unitime.data.local.db.auth.UsersDao
import com.example.unitime.data.local.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

enum class JoinResult {
    SUCCESS,
    ALREADY_JOINED,
    FULL,
    USER_NOT_FOUND
}

class EventParticipantRepository(
    private val participantDao: EventParticipantDao,
    private val usersDao: UsersDao
) {

    fun observeParticipantCount(eventId: Long): Flow<Int> {
        return participantDao.observeParticipantCount(eventId)
    }

    fun observeJoinedEventsCount(userLogin: String): Flow<Int> = flow {
        val user = usersDao.getUserByLogin(userLogin)
        if (user != null) {
            emitAll(participantDao.observeJoinedEventsCount(user.id))
        } else {
            emit(0)
        }
    }

    suspend fun joinEvent(
        eventId: Long,
        userLogin: String
    ): JoinResult {
        val user = usersDao.getUserByLogin(userLogin)
            ?: return JoinResult.USER_NOT_FOUND

        if (participantDao.isParticipant(eventId, user.id)) {
            return JoinResult.ALREADY_JOINED
        }

        val joined = participantDao.joinIfAvailable(eventId, user.id)

        if (joined) return JoinResult.SUCCESS

        val count = participantDao.getParticipantCount(eventId)
        val limit = participantDao.getParticipantLimit(eventId) ?: 0

        return if (count >= limit) {
            JoinResult.FULL
        } else {
            JoinResult.ALREADY_JOINED
        }
    }

    suspend fun leaveEvent(
        eventId: Long,
        userLogin: String
    ) {
        val user = usersDao.getUserByLogin(userLogin) ?: return
        participantDao.leaveEvent(eventId, user.id)
    }

    suspend fun hasJoined(
        eventId: Long,
        userLogin: String
    ): Boolean {
        val user = usersDao.getUserByLogin(userLogin)
            ?: return false

        return participantDao.isParticipant(eventId, user.id)
    }

    // НОВОЕ: Получаем список участников мероприятия
    suspend fun getParticipantsList(eventId: Long): List<User> {
        return withContext(Dispatchers.IO) {
            // 1. Получаем ID участников из EventsDb
            val userIds = participantDao.getUserIdsForEvent(eventId)

            // 2. Для каждого ID запрашиваем полные данные из UsersDb
            userIds.mapNotNull { userId ->
                usersDao.getUserById(userId)
            }
        }
    }
}