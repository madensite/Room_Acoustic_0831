package com.example.roomacoustic.screens.measure

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roomacoustic.navigation.Screen
import com.example.roomacoustic.viewmodel.RoomViewModel
import com.example.roomacoustic.model.Vec3

@Composable
fun RenderScreen(
    nav: NavController,
    vm: RoomViewModel,      // Activity-Scoped ViewModel
    detected: Boolean
) {
    val roomId = vm.currentRoomId.collectAsState().value
    if (roomId == null) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        return
    }

    // 저장된 길이(라벨) & 스피커 & (있다면) 6점 측정 좌표계
    val labeled = vm.labeledMeasures.collectAsState().value
    val speakers = vm.speakers
    val frame3D = vm.measure3DResult.collectAsState().value   // 있을 수도, 없을 수도

    // 배너 색상: 탐지됨=primary, 아니면 error (중앙 배너는 눈에 띄는 색)
    val bannerColor = if (detected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.error

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        /* 중앙 배너 — 가시성 강조 */
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = if (detected) "스피커 탐지 완료" else "스피커 미탐지",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = bannerColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "감지된 스피커: ${speakers.size}개",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground  // 흰색 테마라면 흰색에 가깝게 표시됨
            )
        }

        /* 좌측 상단 — 측정값(라벨링된 길이) */
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 24.dp)
        ) {
            Text("측정값", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            if (labeled.isEmpty()) {
                Text("저장된 길이 측정값이 없습니다.", color = MaterialTheme.colorScheme.onBackground)
            } else {
                Column {
                    labeled.forEach { m ->
                        Text("• ${m.label}: ${"%.2f".format(m.meters)} m", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        /* 좌측 하단 — 스피커 좌표(월드 / 로컬 변환) */
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 80.dp) // 버튼과 겹치지 않도록
        ) {
            Text("스피커 위치", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            if (speakers.isEmpty()) {
                Text("감지된 스피커 없음", color = MaterialTheme.colorScheme.onBackground)
            } else {
                val toLocal: (FloatArray) -> Triple<Float, Float, Float>? = { p ->
                    val m = frame3D
                    if (m == null) {
                        null
                    } else {
                        val origin = m.frame.origin
                        val vx = m.frame.vx
                        val vy = m.frame.vy
                        val vz = m.frame.vz
                        val d = Vec3(p[0], p[1], p[2]) - origin
                        Triple(d.dot(vx), d.dot(vy), d.dot(vz))
                    }
                }

                Spacer(Modifier.height(4.dp))
                speakers.forEachIndexed { idx, sp ->
                    val w = sp.worldPos
                    val worldStr = "(${fmt(w[0])}, ${fmt(w[1])}, ${fmt(w[2])}) m"
                    val local = toLocal(w)
                    val localStr = local?.let { "(W,D,H): (${fmt(it.first)}, ${fmt(it.third)}, ${fmt(it.second)}) m" }
                    // 주의: 우리 축 의미에 맞게 W=X, D=Z, H=Y로 매핑
                    Text("• #${idx+1} world: $worldStr", color = MaterialTheme.colorScheme.onBackground)
                    if (local != null) {
                        Text("    → local $localStr", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        /* 우하단 — 다음 버튼 */
        Button(
            onClick = { nav.navigate(Screen.TestGuide.route) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 50.dp)
        ) { Text("다음") }
    }
}

/* 숫자 포맷 유틸 */
private fun fmt(v: Float) = String.format("%.2f", v)

/* Vec3 연산 유틸 (로컬 변환용) */
private operator fun Vec3.minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
private fun Vec3.dot(o: Vec3) = x * o.x + y * o.y + z * o.z
