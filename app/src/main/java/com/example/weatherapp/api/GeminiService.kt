package com.example.weatherapp.api

import com.example.weatherapp.model.GeminiRequest
import com.example.weatherapp.model.GeminiResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

private val geminiRetrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl("https://generativelanguage.googleapis.com/")
    .build()

interface GeminiService {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun getWeatherSummary(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApi {
    val service: GeminiService by lazy {
        geminiRetrofit.create(GeminiService::class.java)
    }
}