package com.example.roomacoustic.tracker

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * IoU 계산 대신 3-D 거리로만 매칭하는 가벼운 Tracker.
 *
 * - 새 좌표와 기존 좌표 거리가 threshold 이내면 같은 ID 로 간주
 * - 그렇지 않으면 새 ID 발급
 */
object SimpleTracker {
    private val prevPos = mutableMapOf<Int, FloatArray>()
    private var nextId  = 0

    /** threshold: 같은 물체로 인정할 최대 거리(m). 0.20 ≒ 20 cm */
    private const val MERGE_DIST = 0.20f

    fun assignId(pos: FloatArray): Int {
        val match = prevPos.minByOrNull { (_, p) -> dist(p, pos) }
        return if (match != null && dist(match.value, pos) < MERGE_DIST) {
            prevPos[match.key] = pos      // 위치 갱신
            match.key
        } else {
            val id = nextId++
            prevPos[id] = pos
            id
        }
    }

    private fun dist(a: FloatArray, b: FloatArray): Float =
        sqrt((a[0] - b[0]).pow(2) + (a[1] - b[1]).pow(2) + (a[2] - b[2]).pow(2))
}
