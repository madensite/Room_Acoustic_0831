package com.example.roomacoustic.screens.measure

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.roomacoustic.navigation.Screen
import com.example.roomacoustic.viewmodel.RoomViewModel

@Composable
fun MeasureWidthScreen(nav: NavController, vm: RoomViewModel) =
    TwoPointMeasureScreen(
        nav = nav,
        title = "폭 측정 (왼쪽 벽 ↔ 오른쪽 벽)",
        labelKey = "폭",
        nextRoute = Screen.MeasureDepth.route,   // ← 문자열 대신 route 상수
        onSave = { vm.addLabeledMeasure("폭", it) }
    )
