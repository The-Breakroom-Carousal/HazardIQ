package com.hazardiqplus

import com.hazardiqplus.data.SosRequest
import com.hazardiqplus.data.SosResponse
import com.hazardiqplus.data.User
import retrofit2.Call;
import com.hazardiqplus.data.UserRegisterRequest
import com.hazardiqplus.data.UserRegisterResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST ("api/register")
    fun registerUser (@Body registerRequest: UserRegisterRequest): Call<UserRegisterResponse>

    @GET("api/me")
    fun getUserDetails(@Header("idtoken") idToken: String): Call<User>

    @POST("api/send-sos")
    fun sendSosAlert(@Body sendSosRequest: SosRequest): Call<SosResponse>
}