package com.example.roomacoustic.model

data class ChatMessage(
    val sender: String, // "user" or "gpt"
    val content: String
)