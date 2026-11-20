package com.example.unitime.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UniTimeApi {
    @GET("get_users.php")
    suspend fun getAllUsers(): Response<List<UserDto>>

    @FormUrlEncoded
    @POST("register.php")
    suspend fun registerUser(
        @Field("login") login: String,
        @Field("password") password: String,
        @Field("email") email: String,
        @Field("name") name: String,
        @Field("surname") surname: String
    ): Response<RegisterResponse>

    @FormUrlEncoded
    @POST("login.php")
    suspend fun loginUser(
        @Field("login") login: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @Multipart
    @POST("update_profile.php")
    suspend fun updateProfile(
        @Part("login") login: RequestBody,
        @Part("name") name: RequestBody,
        @Part("surname") surname: RequestBody,
        @Part("telegram") telegram: RequestBody?,
        @Part("phone") phone: RequestBody?,
        @Part avatar: MultipartBody.Part?
    ): Response<UpdateProfileResponse>

    @GET("get_events.php")
    suspend fun getAllEvents(): Response<List<EventDto>>

    @FormUrlEncoded
    @POST("create_event.php")
    suspend fun createEvent(
        @Field("title") title: String,
        @Field("description") description: String,
        @Field("category") category: String,
        @Field("date") date: Long,
        @Field("location") location: String,
        @Field("participants_limit") participantsLimit: Int,
        @Field("created_by_login") createdByLogin: String
    ): Response<CreateEventResponse>

    // ✅ ВАЖНО: метод называется deleteEvent (без OnServer)
    @FormUrlEncoded
    @POST("delete_event.php")
    suspend fun deleteEvent(
        @Field("event_id") eventId: Long
    ): Response<ResponseBody>

    @GET("get_event_participants.php")
    suspend fun getEventParticipants(
        @Query("event_id") eventId: Long
    ): Response<ParticipantsResponse>

    @FormUrlEncoded
    @POST("join_event.php")
    suspend fun joinEventOnServer(
        @Field("event_id") eventId: Long,
        @Field("user_login") userLogin: String
    ): Response<JoinEventResponse>

    @FormUrlEncoded
    @POST("leave_event.php")
    suspend fun leaveEventOnServer(
        @Field("event_id") eventId: Long,
        @Field("user_login") userLogin: String
    ): Response<JoinEventResponse>
    @GET("get_user.php")
    suspend fun getUserByLogin(
        @Query("login") login: String
    ): Response<UserProfileResponse>
    @Multipart
    @POST("upload_avatar.php")
    suspend fun uploadAvatar(
        @Part("login") login: RequestBody,
        @Part avatar: MultipartBody.Part
    ): Response<ResponseBody>
}
data class UserProfileResponse(
    val success: Boolean,
    val error: String? = null,
    val user: UserDto? = null
)
data class RegisterResponse(val success: Boolean, val message: String? = null, val error: String? = null, val user_id: Int? = null)
data class LoginResponse(val success: Boolean, val message: String? = null, val error: String? = null, val user: UserDto? = null)
data class JoinEventResponse(val success: Boolean, val error: String? = null, val message: String? = null)
data class UpdateProfileResponse(val success: Boolean, val message: String? = null, val error: String? = null, val avatar_url: String? = null)
data class CreateEventResponse(val success: Boolean, val message: String? = null, val error: String? = null, val event_id: Int? = null)
data class ParticipantsResponse(val success: Boolean, val count: Int, val participants: List<UserDto>)