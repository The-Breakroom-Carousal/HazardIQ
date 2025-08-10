package com.hazardiqplus.services

import com.hazardiqplus.data.AiChatRequest
import com.hazardiqplus.data.AiChatResponse
import com.hazardiqplus.data.ChatHistoryItemDto
import com.hazardiqplus.data.DeleteSosResponse
import com.hazardiqplus.data.FcmTokenUpdateRequest
import com.hazardiqplus.data.FindHazardResponse
import com.hazardiqplus.data.PredictRequest
import com.hazardiqplus.data.PredictResponse
import com.hazardiqplus.data.SaveHazardRequest
import com.hazardiqplus.data.SaveHazardResponse
import com.hazardiqplus.data.SosEvent
import com.hazardiqplus.data.SosRequest
import com.hazardiqplus.data.SosResponse
import com.hazardiqplus.data.UpdateProgressRequest
import com.hazardiqplus.data.UpdateProgressResponse
import com.hazardiqplus.data.User
import com.hazardiqplus.data.UserName
import com.hazardiqplus.data.UserRegisterRequest
import com.hazardiqplus.data.UserRegisterResponse
import com.hazardiqplus.data.UserResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/register")
    fun registerUser (@Body registerRequest: UserRegisterRequest): Call<UserRegisterResponse>
    @GET("api/me")
    fun getUserDetails(@Header("idtoken") idToken: String): Call<User>

    @POST("api/send-sos")
    fun sendSosAlert(@Body sendSosRequest: SosRequest): Call<SosResponse>

    @POST("api/predict-air-quality")
    fun predictAirQuality(@Body predictRequest: PredictRequest): Call<PredictResponse>

    @POST("api/save-hazard")
    fun registerHazard(@Body saveHazardRequest: SaveHazardRequest): Call<SaveHazardResponse>

    @GET("api/find-hazard")
    fun findHazard(
        @Query("lat") lat: Double?,
        @Query("lon") lon: Double?,
        @Query("radius") radius: Int
    ): Call<FindHazardResponse>

    @GET("api/sos-events/{city}")
    fun getSosEvents(@Path("city") city: String): Call<List<SosEvent>>

    @DELETE("api/sos-events/{id}")
    fun deleteSosEvent(@Path("id") id: Int): Call<DeleteSosResponse>

    @PUT("api/sos-events/{id}/progress")
    fun updateSosProgress(
        @Path("id") id: Int,
        @Body request: UpdateProgressRequest
    ): Call<UpdateProgressResponse>

    @PUT("api/user")
    fun updateUser(
        @Header("idtoken") idToken: String,
        @Body request: FcmTokenUpdateRequest
    ): Call<UserResponse>

    @GET("api/get-name")
    fun getUserName(@Header("uid") uid: String): Call<UserName>


    @PUT("api/ai-chat")
    fun sendMessage(
        @Header("idtoken") idToken: String,
        @Body request: AiChatRequest
    ): Call<AiChatResponse> // Use the correct response type

    @GET("api/ai-chat/history")
    fun getHistory(
        @Header("idtoken") idToken: String
    ): Call<ChatHistoryResponse>

    @POST("api/ai-chat/restart")
    fun restartChat(
        @Header("idtoken") idToken: String
    ): Call<Void>

}
data class ChatHistoryResponse(val success: Boolean, val history: List<ChatHistoryItemDto>)