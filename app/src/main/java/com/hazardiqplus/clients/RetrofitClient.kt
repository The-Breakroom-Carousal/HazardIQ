package com.hazardiqplus.clients

import com.hazardiqplus.services.ApiService
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.hazardiqplus.services.MedRecommenderApi

object RetrofitClient {
    private const val BACKEND_BASE_URL = "https://hazardiq-bwxg.onrender.com/"

    val backendInstance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BACKEND_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
            .create(ApiService::class.java)
    }

    private const val MED_BASE_URL = "https://arnavtr1-hazardiq-medrecommender.hf.space/"

    val medInstance: MedRecommenderApi by lazy {
        Retrofit.Builder()
            .baseUrl(MED_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MedRecommenderApi::class.java)
    }
}