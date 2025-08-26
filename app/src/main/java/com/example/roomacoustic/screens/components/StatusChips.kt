package com.example.roomacoustic.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StatusChips(hasMeasure: Boolean, hasChat: Boolean) {
    Column {
        AssistChip(
            onClick = {},
            label = { Text("측정") },
            colors = if (hasMeasure)
                AssistChipDefaults.assistChipColors(
                    containerColor = Color.Red,
                    labelColor = Color.White            // ★ 글씨 흰색
                )
            else AssistChipDefaults.assistChipColors(
                labelColor = Color.Gray                // 선택 안 됐을 때 회색 글씨 (옵션)
            )
        )
        Spacer(Modifier.height(4.dp))
        AssistChip(
            onClick = {},
            label = { Text("대화") },
            colors = if (hasChat)
                AssistChipDefaults.assistChipColors(
                    containerColor = Color.Green,
                    labelColor = Color.White
                )
            else AssistChipDefaults.assistChipColors(
                labelColor = Color.Gray
            )
        )
    }
}
