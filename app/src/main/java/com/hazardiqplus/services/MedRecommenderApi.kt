package com.hazardiqplus.services

import com.hazardiqplus.data.PredictionEventResponse
import com.hazardiqplus.data.PredictionRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MedRecommenderApi {
    @POST("gradio_api/call/predict")
    fun startPrediction(
        @Body request: PredictionRequest
    ): Call<PredictionEventResponse>

    @GET("gradio_api/call/predict/{event_id}")
    fun getPredictionResult(
        @Path("event_id") eventId: String
    ): Call<ResponseBody>
}