package com.hazardiqplus.services

import com.hazardiqplus.data.AQIResponse
import com.hazardiqplus.data.NearbyAQIResponse
import com.hazardiqplus.data.PredictRequest
import com.hazardiqplus.data.PredictResponse
import com.hazardiqplus.data.SosRequest
import com.hazardiqplus.data.SosResponse
import com.hazardiqplus.data.User
import com.hazardiqplus.data.UserRegisterRequest
import com.hazardiqplus.data.UserRegisterResponse
import com.hazardiqplus.data.WeatherResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/register")
    fun registerUser (@Body registerRequest: UserRegisterRequest): Call<UserRegisterResponse>

    @GET("api/me")
    fun getUserDetails(@Header("idtoken") idToken: String): Call<User>

    @POST("api/send-sos")
    fun sendSosAlert(@Body sendSosRequest: SosRequest): Call<SosResponse>

    @POST("api/predict-air-quality")
    fun predictAirQuality(
        @Body predictRequest: PredictRequest
    ): Call<PredictResponse>
}