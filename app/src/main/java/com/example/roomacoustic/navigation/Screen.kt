package com.example.roomacoustic.navigation

sealed class Screen(val route: String) {

    /* ─────────  기존 화면  ───────── */
    object Splash : Screen("splash")
    object Room   : Screen("room")

    /* ─────────  챗봇 쪽  ───────── */
    object NewChat : Screen("newChat/{roomId}")
    object ExChat  : Screen("exChat/{roomId}")

    /* ─────────  ★ 측정 플로우 ★  ───────── */
    /** 서브그래프(Measure Flow)에 진입할 때 쓰는 가상 라우트 */
    object MeasureGraph : Screen("measureGraph")

    /** 4개 단계 + 이후 화면 */
    object MeasureWidth   : Screen("measureWidth")     // ① 폭 측정
    object MeasureDepth   : Screen("measureDepth")     // ② 깊이 측정
    object MeasureHeight  : Screen("measureHeight")    // ③ 높이 측정
    object DetectSpeaker  : Screen("detectSpeaker")    // ④ 스피커 탐지

    object Render    : Screen("render")                // 시각화
    object TestGuide : Screen("testGuide")             // 녹음 가이드
    object KeepTest  : Screen("keepTest")              // 녹음 진행
    object Analysis  : Screen("analysis/{roomId}")     // 결과 분석(기존 그대로 유지)
}
