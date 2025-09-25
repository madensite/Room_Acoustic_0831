package com.example.roomacoustic.screens.measure

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.roomacoustic.navigation.Screen
import com.example.roomacoustic.viewmodel.RoomViewModel

@Composable
fun MeasureDepthScreen(nav: NavController, vm: RoomViewModel) =
    TwoPointMeasureScreen(
        nav = nav,
        title = "깊이 측정 (앞 벽 ↔ 뒤 벽)",
        labelKey = "깊이",
        nextRoute = Screen.MeasureHeight.route,  // ← 수정
        onSave = { vm.addLabeledMeasure("깊이", it) }
    )
