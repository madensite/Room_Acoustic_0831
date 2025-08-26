package com.example.roomacoustic.model

data class GPTRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<Message>,
    val temperature: Double = 0.7
)