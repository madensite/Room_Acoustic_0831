package com.example.roomacoustic.util

import android.content.Context

object PromptLoader {
    fun load(context: Context, filename: String = "prompt/prompt001.txt"): String =
        context.assets.open(filename).bufferedReader().use { it.readText() }
}
