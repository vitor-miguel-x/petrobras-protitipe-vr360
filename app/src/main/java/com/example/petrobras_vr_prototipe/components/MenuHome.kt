package com.example.petrobras_vr_prototipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import com.example.petrobras_vr_prototipe.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.petrobras_vr_prototipe.screens.VrAppState

@Composable
fun MenuHome(
    isNetworkingOpen: Boolean,
    isTasksOpen: Boolean,
    isSettingsOpen: Boolean,
    onNavigate: (VrAppState) -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    // Verde Petrobras para os botões ativos
    val activeColor = Color(0xFF008A52)
    val inactiveColor = Color.Transparent

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                slideInVertically(initialOffsetY = { it / 2 }),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                // Aumentei a largura e altura para caber os ícones maiores
                .width(360.dp)
                .height(70.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(R.drawable.icone_petrobras),
                contentDescription = "Icone Petrobras",
                modifier = Modifier.size(40.dp) // Tamanho aumentado
            )

            // Botão Tarefas
            Row(modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isTasksOpen) activeColor else inactiveColor)
                .clickable { onNavigate(VrAppState.TASKS) }
                .padding(8.dp), // Padding interno do quadrado verde aumentado
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icone_tarefas),
                    contentDescription = "Icone Tarefas",
                    modifier = Modifier.size(40.dp) // Tamanho aumentado
                )
            }

            // Botão Networking
            Row(modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isNetworkingOpen) activeColor else inactiveColor)
                .clickable { onNavigate(VrAppState.NETWORKING) }
                .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icone_networking),
                    contentDescription = "Icone Networking",
                    modifier = Modifier.size(40.dp) // Tamanho aumentado
                )
            }

            // Botão Settings
            Row(modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSettingsOpen) activeColor else inactiveColor)
                .clickable { onNavigate(VrAppState.SETTINGS) }
                .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icone_settings),
                    contentDescription = "Icone Configurações",
                    modifier = Modifier.size(40.dp) // Tamanho aumentado
                )
            }
        }
    }
}