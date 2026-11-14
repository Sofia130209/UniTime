package com.example.unitime.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = EventTagCrossRef.TABLE_NAME,
    primaryKeys = [EventTagCrossRef.EVENT_ID_COLUMN, EventTagCrossRef.TAG_ID_COLUMN],
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = [Event.ID_COLUMN],
            childColumns = [EventTagCrossRef.EVENT_ID_COLUMN],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = [Tag.Companion.ID_COLUMN],
            childColumns = [EventTagCrossRef.TAG_ID_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(EventTagCrossRef.EVENT_ID_COLUMN), Index(EventTagCrossRef.TAG_ID_COLUMN)]
)
data class EventTagCrossRef(
    @ColumnInfo(name = EVENT_ID_COLUMN) val eventId: Long,
    @ColumnInfo(name = TAG_ID_COLUMN) val tagId: Long
) {
    companion object {
        const val TABLE_NAME = "event_tag_cross_ref"
        const val EVENT_ID_COLUMN = "event_id"
        const val TAG_ID_COLUMN = "tag_id"
    }
}