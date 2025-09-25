package com.example.roomacoustic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.navigation
import androidx.navigation.NavType
import androidx.navigation.navArgument

import com.example.roomacoustic.navigation.Screen
import com.example.roomacoustic.screens.RoomScreen
import com.example.roomacoustic.screens.SplashScreen
import com.example.roomacoustic.ui.theme.RoomacousticTheme
import com.example.roomacoustic.screens.chat.ChatScreen

// ▼ 측정/탐지 화면들
import com.example.roomacoustic.screens.measure.MeasureWidthScreen
import com.example.roomacoustic.screens.measure.MeasureDepthScreen
import com.example.roomacoustic.screens.measure.MeasureHeightScreen
import com.example.roomacoustic.screens.measure.DetectSpeakerScreen
import com.example.roomacoustic.screens.measure.RenderScreen
import com.example.roomacoustic.screens.measure.TestGuideScreen
import com.example.roomacoustic.screens.measure.KeepTestScreen
import com.example.roomacoustic.screens.measure.AnalysisScreen

import com.example.roomacoustic.viewmodel.RoomViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    RoomacousticTheme {
        val nav = rememberNavController()
        val vm: RoomViewModel = viewModel()

        NavHost(navController = nav, startDestination = Screen.Splash.route) {
            composable(Screen.Splash.route) { SplashScreen(nav) }
            composable(Screen.Room.route)   { RoomScreen(nav, vm) }

            /* ② ChatScreens ------------------------------------------------ */
            composable(
                route = Screen.NewChat.route,
                arguments = listOf(navArgument("roomId") { type = NavType.IntType })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments!!.getInt("roomId")
                ChatScreen(nav, roomId)
            }

            composable(
                route = Screen.ExChat.route,
                arguments = listOf(navArgument("roomId") { type = NavType.IntType })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments!!.getInt("roomId")
                ChatScreen(nav, roomId)
            }

            // ── 측정 플로우 서브그래프 ──
            navigation(
                startDestination = Screen.MeasureWidth.route,  // ★ 폭 → 깊이 → 높이 → 탐지
                route = Screen.MeasureGraph.route
            ) {
                composable(Screen.MeasureWidth.route)  { MeasureWidthScreen(nav, vm) }
                composable(Screen.MeasureDepth.route)  { MeasureDepthScreen(nav, vm) }
                composable(Screen.MeasureHeight.route) { MeasureHeightScreen(nav, vm) }
                composable(Screen.DetectSpeaker.route) { DetectSpeakerScreen(nav, vm) }

                composable(
                    route = "${Screen.Render.route}?detected={detected}",
                    arguments = listOf(
                        navArgument("detected") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val detected = backStackEntry.arguments?.getBoolean("detected") ?: false
                    RenderScreen(nav = nav, vm = vm, detected = detected)
                }

                composable(Screen.TestGuide.route) { TestGuideScreen(nav, vm) }
                composable(Screen.KeepTest.route)  { KeepTestScreen(nav, vm) }

                // NOTE: Analysis 라우트는 "analysis/{roomId}" 형식이라면 argument 선언해도 좋음.
                // 지금처럼 다른 곳에서만 진입하고 여기서 직접 push 안 하면 그대로 둬도 OK.
                composable(Screen.Analysis.route)  { AnalysisScreen(nav, vm) }
            }
        }
    }
}
