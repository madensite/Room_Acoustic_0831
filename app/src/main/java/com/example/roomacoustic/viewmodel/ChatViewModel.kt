package com.example.roomacoustic.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.example.roomacoustic.BuildConfig
import com.example.roomacoustic.model.*
import com.example.roomacoustic.util.RetrofitClient

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    /** 시스템 프롬프트를 호출 쪽에서 넘겨받도록 변경 */
    fun sendPrompt(
        systemPrompt: String,
        userText: String,
        onError: (String) -> Unit
    ) {
        append("user", userText)

        val request = GPTRequest(
            messages = listOf(
                Message("system", systemPrompt),
                Message("user", userText)
            )
        )
        val token = "Bearer ${BuildConfig.OPENAI_API_KEY}"

        RetrofitClient.api.sendPrompt(token, request).enqueue(object : Callback<GPTResponse> {
            override fun onResponse(call: Call<GPTResponse>, resp: Response<GPTResponse>) {
                if (resp.isSuccessful) {
                    resp.body()?.choices?.firstOrNull()?.message?.content
                        ?.let { append("gpt", it) }
                        ?: append("gpt", "⚠️ GPT 응답이 비었습니다.")
                } else onError("OpenAI 오류: ${resp.code()}")
            }
            override fun onFailure(call: Call<GPTResponse>, t: Throwable) =
                onError("네트워크 오류: ${t.message}")
        })
    }

    private fun append(sender: String, content: String) {
        _messages.update { it + ChatMessage(sender, content) }
    }
}