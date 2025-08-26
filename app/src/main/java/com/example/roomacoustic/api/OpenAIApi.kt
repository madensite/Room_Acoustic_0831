package com.example.roomacoustic.api

import com.example.roomacoustic.model.GPTRequest
import com.example.roomacoustic.model.GPTResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIApi {
    @POST("v1/chat/completions")
    fun sendPrompt(
        @Header("Authorization") auth: String,
        @Body request: GPTRequest
    ): Call<GPTResponse>
}