package com.example.unitime.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unitime.data.local.model.EventParticipant
import kotlinx.coroutines.flow.Flow

@Dao
interface EventParticipantDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(participant: EventParticipant)

    @Query("DELETE FROM event_participants WHERE event_id = :eventId AND user_login = :userLogin")
    suspend fun delete(eventId: Long, userLogin: String): Int

    @Query("SELECT COUNT(*) FROM event_participants WHERE event_id = :eventId")
    fun observeParticipantCount(eventId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM event_participants WHERE event_id = :eventId")
    suspend fun getParticipantCount(eventId: Long): Int

    @Query("SELECT COUNT(*) FROM event_participants WHERE event_id = :eventId AND user_login = :userLogin")
    suspend fun hasJoined(eventId: Long, userLogin: String): Int

    @Query("SELECT COUNT(DISTINCT event_id) FROM event_participants WHERE user_login = :userLogin")
    fun observeJoinedEventsCount(userLogin: String): Flow<Int>

    @Query("SELECT user_login FROM event_participants WHERE event_id = :eventId")
    suspend fun getUserLoginsForEvent(eventId: Long): List<String>
}