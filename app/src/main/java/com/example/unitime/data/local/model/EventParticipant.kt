package com.example.unitime.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "event_participants",
    primaryKeys = ["event_id", "user_id"],
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = [Event.ID_COLUMN],
            childColumns = ["event_id"],
            _root_ide_package_.com.example.unitime.data.local.model.Event = _root_ide_package_.androidx.room.ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        _root_ide_package_.androidx.room.Index(value = ["user_id"])
    ]
)
data class EventParticipant(
    @androidx.room.ColumnInfo(name = "event_id")
    val eventId: kotlin.Long,

    @androidx.room.ColumnInfo(name = "user_id")
    val userId: kotlin.Long
)
