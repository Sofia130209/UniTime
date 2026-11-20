package com.example.unitime.data.mapper

import com.example.unitime.data.local.model.Event
import com.example.unitime.data.local.model.User
import com.example.unitime.data.remote.EventDto
import com.example.unitime.data.remote.UserDto

// --- ПОЛЬЗОВАТЕЛЬ ---
fun UserDto.toEntity(): User {
    return User(
        id = this.id.toLong(),
        login = this.login,
        name = this.name ?: "",
        surname = this.surname ?: "",
        email = this.email ?: "",
        telegram = this.telegram,
        phone = this.phone,
        password = "",
        avatarPath = null
    )
}

fun User.toDto(): UserDto {
    return UserDto(
        id = this.id.toInt(),
        login = this.login,
        name = this.name,
        surname = this.surname,
        email = this.email,
        telegram = this.telegram,
        phone = this.phone,
        avatarUrl = null
    )
}

// --- СОБЫТИЯ ---
fun EventDto.toEntity(): Event {
    return Event(
        id = this.id,
        activityTitle = this.activityTitle ?: "Без названия",
        activityDescription = this.activityDescription ?: "",
        activityCategory = this.activityCategory ?: "Другое",
        activityDate = this.activityDate,
        activityLocation = this.activityLocation ?: "",
        activityParticipants = this.activityParticipants,
        createdByLogin = this.createdByLogin ?: ""
    )
}

fun Event.toDto(): EventDto {
    return EventDto(
        id = this.id,
        activityTitle = this.activityTitle ?: "",
        activityDescription = this.activityDescription ?: "",
        activityCategory = this.activityCategory ?: "",
        activityDate = this.activityDate,
        activityLocation = this.activityLocation ?: "",
        activityParticipants = this.activityParticipants,
        createdByLogin = this.createdByLogin ?: "",
        currentParticipants = 0 // <-- ДОБАВЛЕНО: обязательное поле для EventDto
    )
}