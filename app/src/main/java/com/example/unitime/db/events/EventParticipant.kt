package com.example.unitime.db.events

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.unitime.db.events.Event

@Entity(
    tableName = "event_participants",
    primaryKeys = ["event_id", "user_id"],
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = [Event.ID_COLUMN],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"])
    ]
)
data class EventParticipant(
    @ColumnInfo(name = "event_id")
    val eventId: Long,

    @ColumnInfo(name = "user_id")
    val userId: Long
)
