package com.example.unitime.data.remote

data class UserDto(
    val id: Int,
    val login: String,
    val name: String?,
    val surname: String?,
    val email: String?,
    val telegram: String?,
    val phone: String?,
    val avatarUrl: String? = null
)