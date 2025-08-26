package com.example.roomacoustic.model

data class GPTResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
