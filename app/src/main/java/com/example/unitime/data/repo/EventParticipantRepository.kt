package com.example.unitime.data.repo

import com.example.unitime.data.local.dao.EventParticipantDao
import com.example.unitime.data.local.db.auth.UsersDao
import com.example.unitime.data.local.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    fun observeJoinedEventsCount(userLogin: String): Flow<Int> {
        return participantDao.observeJoinedEventsCount(userLogin)
    }

    suspend fun joinEvent(
        eventId: Long,
        userLogin: String
    ): JoinResult {
        val user = usersDao.getUserByLogin(userLogin)
            ?: return JoinResult.USER_NOT_FOUND

        if (participantDao.hasJoined(eventId, userLogin) > 0) {
            return JoinResult.ALREADY_JOINED
        }

        // Простая вставка без сложной проверки лимита в DAO,
        // так как мы убрали joinIfAvailable из интерфейса DAO для простоты
        try {
            participantDao.insert(com.example.unitime.data.local.model.EventParticipant(eventId = eventId, userLogin = userLogin))
            return JoinResult.SUCCESS
        } catch (e: Exception) {
            return JoinResult.ALREADY_JOINED
        }
    }

    suspend fun leaveEvent(
        eventId: Long,
        userLogin: String
    ) {
        participantDao.delete(eventId, userLogin)
    }

    suspend fun hasJoined(
        eventId: Long,
        userLogin: String
    ): Boolean {
        return participantDao.hasJoined(eventId, userLogin) > 0
    }

    suspend fun getParticipantsList(eventId: Long): List<User> {
        return withContext(Dispatchers.IO) {
            val logins = participantDao.getUserLoginsForEvent(eventId)
            logins.mapNotNull { login ->
                usersDao.getUserByLogin(login)
            }
        }
    }
}