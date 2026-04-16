package com.example.petrobras_vr_prototipe

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.example.petrobras_vr_prototipe.screens.CameraScreen
import com.example.petrobras_vr_prototipe.ui.theme.PetrobrasVrPrototipeTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configurações de Fullscreen (Imersivo)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())

        enableEdgeToEdge()

        setContent {
            PetrobrasVrPrototipeTheme {
                // A CameraScreen agora é a única responsável por gerenciar seu próprio estado de login
                CameraScreen()
            }
        }
    }
}