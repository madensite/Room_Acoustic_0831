package com.example.roomacoustic.screens.chat   // 원하는 패키지로 조정

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.roomacoustic.model.ChatMessage
import com.example.roomacoustic.viewmodel.ChatViewModel
import com.example.roomacoustic.util.PromptLoader   // ⬅️ 추가
import androidx.compose.runtime.remember            // 이미 있음
import androidx.compose.ui.platform.LocalContext    // ⬅️ 추가



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    nav: NavController,
    roomId: Int,
    vm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    /* ① Context 가져오기 */
    val context = LocalContext.current

    /* ② 자산 파일 → 메모리 (remember 덕분에 재조합 때 다시 안 읽음) */
    val systemPrompt by remember {
        mutableStateOf(PromptLoader.load(context))   // 기본값: "assets/prompt/prompt001.txt"
    }

    val msgs by vm.messages.collectAsState()
    val listState = rememberLazyListState()


    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("") },
                //title = { Text("대화 (방 #$roomId)") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)

        ) {

            /* --- 메시지 리스트 --- */
            LazyColumn(
                state = listState,
                reverseLayout = true,                     // 🔹 아래 → 위 정렬
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(8.dp)
            ) {
                // 🔹 역순 공급해야 말풍선 순서가 올바름
                items(msgs.asReversed()) { ChatBubble(it) }
            }

            /* 새 메시지 추가 시, 항상 '아래쪽(=index 0)'으로 스크롤 */
            LaunchedEffect(msgs.size) {
                listState.animateScrollToItem(0)
            }

            /* --- 입력창 + 전송 버튼 --- */
            var input by remember { mutableStateOf("") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 키보드(IME) + 내비게이션바(BOTTOM)만 합친 패딩
                    .windowInsetsPadding(
                        WindowInsets
                            .ime                       // 키보드
                            .union(WindowInsets.navigationBars)   // + 내비게이션바
                            .only(WindowInsetsSides.Bottom)       // 하단만 적용
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),  // 시각적 여백
                verticalAlignment = Alignment.CenterVertically
            ) {
                /* 슬림한 입력창 */
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)                              // 얇게
                        .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (input.isBlank()) {
                        Text("메시지 입력", color = Color.Gray, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                IconButton(
                    enabled = input.isNotBlank(),
                    onClick = {
                        vm.sendPrompt(                       // (1) systemPrompt 인자 추가
                            systemPrompt = systemPrompt,
                            userText = input,
                            onError = { /* TODO: 에러 처리 */ }
                        )
                        input = ""
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "보내기")
                }

            }
        }
    }
}

/* --------- 단일 메시지 버블 ---------- */
@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.sender == "user"
    val bg = if (isUser) Color(0xFFE0E0E0) else Color(0xFF4CAF50)
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val txtColor = if (isUser) Color.Black else Color.White
    val hPadStart = if (isUser) 52.dp else 8.dp
    val hPadEnd = if (isUser) 8.dp else 52.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = hPadStart, end = hPadEnd, top = 6.dp, bottom = 6.dp),
        contentAlignment = align
    ) {
        Text(
            text = msg.content,
            color = txtColor,
            modifier = Modifier
                .background(bg, shape = MaterialTheme.shapes.medium)
                .padding(10.dp)
        )
    }
}
