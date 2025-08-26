package com.example.roomacoustic.viewmodel

import android.graphics.Bitmap
import android.media.Image
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.roomacoustic.util.AngleUtils
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import kotlin.math.abs

const val STEP_ANGLE    = 45f         // ° 간격
const val ALIGN_TOL_DEG = 4f          // ± 허용 오차
const val HOLD_MS       = 500f        // 정렬 유지 시간(ms)

enum class ScanState { WAIT_POSE, ALIGNING, HOLDING, DONE }

data class Sample(val rgb: Bitmap, val depth: Image, val pose: Pose)

class ScanViewModel : ViewModel() {

    private val _samples = mutableStateListOf<Sample>()
    val samples: List<Sample> = _samples

    /** 진행 상태 */
    var state       by mutableStateOf(ScanState.WAIT_POSE); private set
    var targetYaw   by mutableStateOf(0f);  private set      // 다음 타깃 각도
    var progress    by mutableStateOf(0f);  private set      // 0–1, 링 진행률

    private var baseYawSet   = false
    private var holdElapsed  = 0f            // ms
    private var prevTs       = 0L            // ns
    private var yawSmooth = 0f

    var diffToTarget by mutableStateOf(0f)
        private set



    /** 매 ARCore 프레임마다 호출 */
    fun onFrame(frame: Frame, rgb: Bitmap, depth: Image?) {
        if (state == ScanState.DONE) return

        /* 0. yaw 계산 */
        val yaw = AngleUtils.yawDeg(frame.camera.pose)
        val rawYaw   = AngleUtils.yawDeg(frame.camera.pose)
        yawSmooth    = 0.85f * yawSmooth + 0.15f * rawYaw   // 지터 억제
        if (!baseYawSet) {                        // 첫 프레임 기준 설정
            targetYaw   = (yaw + STEP_ANGLE) % 360f
            baseYawSet  = true
        }
        val diff     = AngleUtils.diffDeg(yawSmooth, targetYaw)
        diffToTarget = diff

        /* 1. 상태 전이 */
        when (state) {
            ScanState.WAIT_POSE -> {
                if (abs(diff) <= ALIGN_TOL_DEG) {
                    state = ScanState.ALIGNING
                    holdElapsed = 0f
                }
            }
            ScanState.ALIGNING -> {
                if (abs(diff) > ALIGN_TOL_DEG) {
                    state = ScanState.WAIT_POSE
                    progress = 0f
                } else {
                    /* 2. 진행 링 업데이트 */
                    val dt = deltaMs(frame)      // ms
                    holdElapsed += dt
                    progress = (holdElapsed / HOLD_MS).coerceIn(0f, 1f)
                    if (holdElapsed >= HOLD_MS) {
                        capture(rgb, depth, frame.camera.pose)
                        advanceTarget()
                    }
                }
            }
            else -> Unit
        }

    }

    private fun capture(rgb: Bitmap, depth: Image?, pose: Pose) {
        if (depth == null) return
        val cfg = rgb.config ?: Bitmap.Config.ARGB_8888
        _samples += Sample(rgb.copy(cfg, false), depth, pose)
    }

    private fun advanceTarget() {
        progress    = 0f
        holdElapsed = 0f
        targetYaw   = (targetYaw + STEP_ANGLE) % 360f
        state       = if (targetYaw < STEP_ANGLE / 2f) ScanState.DONE else ScanState.WAIT_POSE
    }

    private fun deltaMs(frame: Frame): Float {
        val dt = if (prevTs == 0L) 0f else (frame.timestamp - prevTs) / 1_000_000f
        prevTs = frame.timestamp
        return dt
    }

    fun reset() {
        state = ScanState.WAIT_POSE
        targetYaw = 0f
        progress = 0f
        baseYawSet = false
        holdElapsed = 0f
        prevTs = 0L
        _samples.clear()
    }

}
