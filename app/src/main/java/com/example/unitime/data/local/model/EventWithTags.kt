package com.example.unitime.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class EventWithTags(
    @Embedded val event: Event,
    @Relation(
        parentColumn = Event.ID_COLUMN,
        entityColumn = Tag.Companion.ID_COLUMN,
        associateBy = Junction(
            value = EventTagCrossRef::class,
            parentColumn = EventTagCrossRef.EVENT_ID_COLUMN,
            entityColumn = EventTagCrossRef.TAG_ID_COLUMN
        )
    )
    val tags: List<Tag>
)