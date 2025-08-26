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

    /** 5 개 실제 화면 라우트 — 순서대로 이동 */
    object Measure   : Screen("measure")     // ① 카메라 측정
    object Render    : Screen("render")      // ② 시각화
    object TestGuide : Screen("testGuide")   // ③ 녹음 가이드
    object KeepTest  : Screen("keepTest")    // ④ 녹음 진행
    object Analysis  : Screen("analysis/{roomId}")    // ⑤ 결과 분석
}