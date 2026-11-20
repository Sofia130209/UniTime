package com.example.unitime.data.remote

import com.google.gson.annotations.SerializedName

data class EventDto(
    val id: Long,
    @SerializedName("title") val activityTitle: String?,
    @SerializedName("description") val activityDescription: String?,
    @SerializedName("category") val activityCategory: String?,
    @SerializedName("event_date") val activityDate: Long,
    @SerializedName("location") val activityLocation: String?,
    @SerializedName("participants_limit") val activityParticipants: Int,
    @SerializedName("created_by_login") val createdByLogin: String?,
    @SerializedName("current_participants") val currentParticipants: Int
)