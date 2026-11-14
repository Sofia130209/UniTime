package com.example.unitime.db.events

import com.example.unitime.db.auth.UsersDao
import kotlinx.coroutines.flow.Flow

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

    fun observeParticipantCount(eventId: Long): Flow<Int> =
        participantDao.observeParticipantCount(eventId)

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
}