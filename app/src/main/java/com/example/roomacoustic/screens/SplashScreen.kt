package com.example.roomacoustic.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roomacoustic.navigation.Screen
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.roomacoustic.R

@Composable
fun SplashScreen(nav: NavController) {
    LaunchedEffect(Unit) {
        delay(1_000)
        nav.navigate(Screen.Room.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.speaker),  // ✅ drawable 리소스 사용
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Text(
                text = "룸 어쿠스틱 어플",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )

            )
        }
    }
}
