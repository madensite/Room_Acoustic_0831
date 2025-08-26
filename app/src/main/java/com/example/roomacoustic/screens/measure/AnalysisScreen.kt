package com.example.roomacoustic.screens.measure

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.roomacoustic.navigation.Screen
import com.example.roomacoustic.viewmodel.RoomViewModel

@Composable
fun AnalysisScreen(
    nav: NavController,
    vm: RoomViewModel
) {
    val roomId = vm.currentRoomId.collectAsState().value
    if (roomId == null) {                 // ★ 아직 방 선택이 안 됨 → 플레이스홀더
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return }

    Box(Modifier.fillMaxSize()) {
        Text("📊 Analysis", Modifier.align(Alignment.Center))
        Button(
            onClick = {
                vm.setMeasure(roomId, true)          // 측정 완료 저장
                nav.navigate(Screen.Room.route) {
                    popUpTo(Screen.Room.route) { inclusive = false }
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 50.dp)
        ) { Text("완료") }
    }
}
