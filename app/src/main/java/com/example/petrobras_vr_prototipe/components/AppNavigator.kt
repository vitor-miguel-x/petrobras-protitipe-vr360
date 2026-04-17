package com.example.petrobras_vr_prototipe.components

import androidx.compose.runtime.*
import com.example.petrobras_vr_prototipe.screens.CameraScreen
import com.example.petrobras_vr_prototipe.screens.LoadingScreenApp
import kotlinx.coroutines.delay

@Composable
fun AppNavigator() {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000)

        // Finaliza o loading
        isLoading = false
    }

    // Lógica de transição de tela
    if (isLoading) {
        LoadingScreenApp()
    } else {
        CameraScreen()
    }
}