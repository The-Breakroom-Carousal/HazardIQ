package com.hazardiqplus

import retrofit2.Call;
import com.hazardiqplus.data.UserRegisterRequest
import com.hazardiqplus.data.UserRegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST ("api/register")
    fun registerUser (@Body registerRequest: UserRegisterRequest): Call<UserRegisterResponse>
}