package com.example.unitime.data.remote

data class EventDto(
    val id: Long,
    val activityTitle: String,
    val activityDescription: String,
    val activityCategory: String,
    val activityDate: Long, // Если в базе Long, поменяй на Long
    val activityLocation: String,
    val activityParticipants: Int,
    val createdByLogin: String
)