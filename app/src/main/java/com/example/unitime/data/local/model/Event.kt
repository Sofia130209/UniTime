package com.example.unitime.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = Event.TABLE_NAME
)
data class Event(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    val id: Long = 0,

    @ColumnInfo(name = ACTIVITY_TITLE)
    val activityTitle: String,

    @ColumnInfo(name = ACTIVITY_DESCRIPTION, defaultValue = "")
    val activityDescription: String,

    @ColumnInfo(name = ACTIVITY_CATEGORY)
    val activityCategory: String,

    @ColumnInfo(name = ACTIVITY_DATE)
    val activityDate: Long,

    @ColumnInfo(name = ACTIVITY_LOCATION)
    val activityLocation: String,

    @ColumnInfo(name = ACTIVITY_PARTICIPANTS)
    val activityParticipants: Int,

    @ColumnInfo(name = CREATED_BY_LOGIN)
    val createdByLogin: String
) {
    companion object {
        const val TABLE_NAME = "events"

        const val ID_COLUMN = "id"
        const val ACTIVITY_TITLE = "activity_title"
        const val ACTIVITY_DESCRIPTION = "activity_description"
        const val ACTIVITY_CATEGORY = "activity_category"
        const val ACTIVITY_DATE = "activity_date"
        const val ACTIVITY_LOCATION = "activity_location"
        const val ACTIVITY_PARTICIPANTS = "activity_participants"
        const val CREATED_BY_LOGIN = "created_by_login"
    }
}
