package com.example.roomacoustic.model

/**
 * YOLO + Depth API 로 얻은 단일 스피커 좌표.
 *
 * @property id          추적 ID (SimpleTracker 가 부여)
 * @property worldPos    ARCore 월드 좌표계 [x, y, z] (단위: m)
 * @property lastSeenNs  프레임 타임스탬프(ns) — 일정 시간 지나면 제거용
 */
data class Speaker3D(
    val id: Int,
    var worldPos: FloatArray,
    var lastSeenNs: Long
)
