package com.example.roomacoustic.screens.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.roomacoustic.viewmodel.ScanState
import com.example.roomacoustic.util.AngleUtils

@Composable
fun TargetReticle(
    state: ScanState,
    progress: Float,
    diffDeg: Float,           // ★ 새 파라미터
    modifier: Modifier = Modifier
) {
    val sz = 180.dp            // reticle 전체 크기
    Canvas(modifier.size(sz)) {
        val rOuter = size.minDimension / 2f
        val rInner = rOuter / 4f

        /* 1) 회전 좌표계 – targetYaw 방향으로 돌려서 그리기 */
        val angle = when (state) {
            ScanState.WAIT_POSE, ScanState.ALIGNING, ScanState.HOLDING -> -diffDeg     // △1
            else -> 0f
        }
        rotate(angle) {
            // 타깃 외곽 원(흰색)을 화면에 고정하지 말고,
            // 회전된 좌표계에서 위쪽에 위치하게끔 y-offset
            val r = rOuter * 1.3f           // 카메라 중심에서 1.3R 거리

            drawCircle(
                color   = Color.White,
                radius  = rOuter,
                center  = Offset(0f, -r)    // 회전 좌표계 기준 (x=0, y=-r)
            )
        }

        /* 2) 중앙 조준 원은 그대로 */
        drawCircle(Color.Black, radius = rInner, style = Stroke(width = 4f))

        /* 3) 진행 링 */
        if (state == ScanState.ALIGNING) {
            drawArc(
                color = Color.Magenta,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
        }
    }
}
