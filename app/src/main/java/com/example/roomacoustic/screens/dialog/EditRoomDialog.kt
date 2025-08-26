package com.example.roomacoustic.screens.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



/**
 * 방 이름을 새로 입력하거나 수정할 때 사용하는 공용 다이얼로그
 *
 * @param title      다이얼로그 상단 제목
 * @param default    기본 입력값 (수정 모드일 때 기존 방 이름)
 * @param onConfirm  확인 버튼 클릭 시 호출되는 콜백 – 입력 문자열 전달
 * @param onDismiss  다이얼로그 닫기(취소) 콜백
 */
@Composable
fun EditRoomDialog(
    title: String = "방 이름 입력",
    default: String = "",
    onConfirm: (String) -> Boolean,  // ✅ 성공 여부 반환으로 인터페이스 수정
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue(default)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val input = text.text.trim()
                    if (input.isEmpty()) {
                        errorMessage = "방 이름을 입력해주세요."
                    } else {
                        val success = onConfirm(input)
                        if (!success) {
                            errorMessage = "같은 이름의 방이 이미 존재합니다."
                        }
                    }
                }
            ) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        errorMessage = null
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    placeholder = { Text("예) My Studio") }
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall // 필요시 bodySmall 등으로 교체
                    )
                }
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false)
    )
}
