package com.example.unitime.data.remote

import retrofit2.http.GET

interface UniTimeApi {
    @GET("api/users")
    suspend fun getAllUsers(): List<UserDto>
}