package com.example.roomacoustic.util

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 카메라 권한이 없으면 요청 → 허용될 때만 content() 실행
 */
@Composable
fun CameraPermissionGate(
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LaunchedEffect(granted) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    if (granted) content()
    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("카메라 권한이 허용되어야 측정 기능을 사용할 수 있습니다.")
    }
}
