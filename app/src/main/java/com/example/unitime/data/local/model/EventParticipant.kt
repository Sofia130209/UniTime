package com.example.unitime.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = EventParticipant.TABLE_NAME,
    primaryKeys = [EventParticipant.EVENT_ID_COLUMN, EventParticipant.USER_LOGIN_COLUMN]
)
data class EventParticipant(
    @ColumnInfo(name = EventParticipant.EVENT_ID_COLUMN) val eventId: Long,
    @ColumnInfo(name = EventParticipant.USER_LOGIN_COLUMN) val userLogin: String
) {
    companion object {
        const val TABLE_NAME = "event_participants"
        const val EVENT_ID_COLUMN = "event_id"
        const val USER_LOGIN_COLUMN = "user_login"
    }
}