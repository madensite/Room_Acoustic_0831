package com.example.roomacoustic.screens.measure

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.roomacoustic.navigation.Screen
import com.example.roomacoustic.viewmodel.RoomViewModel

@Composable
fun MeasureHeightScreen(nav: NavController, vm: RoomViewModel) =
    TwoPointMeasureScreen(
        nav = nav,
        title = "높이 측정 (바닥 ↔ 천장)",
        labelKey = "높이",
        nextRoute = Screen.DetectSpeaker.route,  // ← 수정
        onSave = { vm.addLabeledMeasure("높이", it) }
    )
