package com.example.roomacoustic.util

import com.example.roomacoustic.api.OpenAIApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val api: OpenAIApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIApi::class.java)
    }
}