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


import com.example.roomacoustic.navigation.Screen
import com.example.roomacoustic.screens.RoomScreen
import com.example.roomacoustic.screens.SplashScreen
import com.example.roomacoustic.ui.theme.RoomacousticTheme
import com.example.roomacoustic.screens.measure.MeasureScreen
import com.example.roomacoustic.screens.measure.RenderScreen
import com.example.roomacoustic.screens.measure.TestGuideScreen
import com.example.roomacoustic.screens.measure.KeepTestScreen
import com.example.roomacoustic.screens.measure.AnalysisScreen
import com.example.roomacoustic.viewmodel.RoomViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.roomacoustic.screens.chat.ChatScreen


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
        NavHost(nav, startDestination = Screen.Splash.route) {
            composable(Screen.Splash.route) { SplashScreen(nav) }
            composable(Screen.Room.route)   { RoomScreen(nav, vm) }

            /* ② ChatScreens ------------------------------------------------ */
            composable(
                route = "${Screen.NewChat.route}",
                arguments = listOf(navArgument("roomId") { type = NavType.IntType })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments!!.getInt("roomId")
                ChatScreen(nav, roomId)                       // ← 새 대화
            }

            composable(
                route = "${Screen.ExChat.route}",
                arguments = listOf(navArgument("roomId") { type = NavType.IntType })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments!!.getInt("roomId")
                ChatScreen(nav, roomId)                       // ← 기존 대화
            }

            // ── 측정 플로우 서브그래프 ──
            navigation(
                startDestination = Screen.Measure.route,
                route = Screen.MeasureGraph.route
            ) {
                composable(Screen.Measure.route) { MeasureScreen(nav, vm) }

                // ⬇⬇⬇ 이 블록만 교체
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
                    RenderScreen(
                        nav = nav,   // ← 기존에 만든 rememberNavController()
                        vm  = vm,    // ← 위에서 만든 Activity-Scoped RoomViewModel
                        detected = detected
                    )
                }
                composable(Screen.TestGuide.route) { TestGuideScreen(nav, vm) }
                composable(Screen.KeepTest.route)  { KeepTestScreen(nav, vm) }
                composable(Screen.Analysis.route)  { AnalysisScreen(nav, vm) }
            }
        }

    }
}