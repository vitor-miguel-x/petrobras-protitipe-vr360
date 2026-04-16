package com.example.petrobras_vr_prototipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.petrobras_vr_prototipe.screens.CameraScreen
import com.example.petrobras_vr_prototipe.ui.theme.PetrobrasVrPrototipeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Diz ao Android que o app vai desenhar por trás das barras de sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Pega o controlador das barras do sistema
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        // 3. Define o comportamento: as barras só aparecem temporariamente se o usuário deslizar o dedo nas bordas da tela
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 4. Efetivamente esconde a barra de status (topo) e a de navegação (baixo)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        enableEdgeToEdge()
        setContent {
            PetrobrasVrPrototipeTheme {
                CameraScreen()
            }
        }
    }
}

