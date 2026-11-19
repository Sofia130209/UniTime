package com.example.unitime.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.unitime.data.local.model.EventParticipant
import kotlinx.coroutines.flow.Flow

@Dao
interface EventParticipantDao {

    @Query("SELECT COUNT(DISTINCT event_id) FROM event_participants WHERE user_id = :userId")
    fun observeJoinedEventsCount(userId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(participant: EventParticipant): Long

    @Query("SELECT COUNT(*) FROM event_participants WHERE event_id = :eventId")
    fun observeParticipantCount(eventId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM event_participants WHERE event_id = :eventId")
    suspend fun getParticipantCount(eventId: Long): Int

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM event_participants
            WHERE event_id = :eventId AND user_id = :userId
        )
    """)
    suspend fun isParticipant(eventId: Long, userId: Long): Boolean

    @Query("SELECT activity_participants FROM events WHERE id = :eventId")
    suspend fun getParticipantLimit(eventId: Long): Int?

    @Transaction
    suspend fun joinIfAvailable(eventId: Long, userId: Long): Boolean {
        val limit = getParticipantLimit(eventId) ?: return false
        val currentCount = getParticipantCount(eventId)

        if (currentCount >= limit) return false

        return insert(
            EventParticipant(
                eventId = eventId,
                userId = userId
            )
        ) != -1L
    }

    @Query("""
        DELETE FROM event_participants
        WHERE event_id = :eventId AND user_id = :userId
    """)
    suspend fun leaveEvent(eventId: Long, userId: Long)
    @Query("SELECT user_id FROM event_participants WHERE event_id = :eventId")
    suspend fun getUserIdsForEvent(eventId: Long): List<Long>
}