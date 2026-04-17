package com.example.petrobras_vr_prototipe.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsOverlay(
    onClose: () -> Unit,
    onResetBiometrics: () -> Unit
) {
    var voiceEnabled by remember { mutableStateOf(true) }
    var arOpacity by remember { mutableStateOf(0.8f) }

    // Definindo as cores principais da Petrobras
    val petrobrasGreen = Color(0xFF008A52)
    val petrobrasYellow = Color(0xFFFFA600)
    val textColorPrimary = Color(0xFF1E1E1E) // Quase preto para leitura
    val textColorSecondary = Color(0xFF555555) // Cinza escuro para subtítulos

    Box(
        modifier = Modifier
            .fillMaxSize() // Ocupa a tela para o overlay
            .background(Color.Black.copy(alpha = 0.2f)), // Overlay de fundo mais suave
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(400.dp) // Ajustado para não cortar conteúdo
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White) // Fundo Branco
                .padding(24.dp)
        ) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = "Config", tint = petrobrasGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configurações do Sistema",
                        color = textColorPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = textColorSecondary)
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))

            // Seção 1: IA e Áudio
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = "Mic", tint = petrobrasGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Assistente Brás (IA)", color = textColorSecondary, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Reconhecimento de Voz Contínuo", color = textColorPrimary, fontSize = 14.sp)
                Switch(
                    checked = voiceEnabled,
                    onCheckedChange = { voiceEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = petrobrasGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seção 2: Interface AR
            Text(text = "Opacidade da Interface AR", color = textColorPrimary, fontSize = 14.sp)
            Slider(
                value = arOpacity,
                onValueChange = { arOpacity = it },
                valueRange = 0.2f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = petrobrasGreen,
                    activeTrackColor = petrobrasGreen,
                    inactiveTrackColor = petrobrasGreen.copy(alpha = 0.24f)
                )
            )

            Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))

            // Seção 3: Segurança
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = "Security", tint = textColorSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Segurança Operacional", color = textColorSecondary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onResetBiometrics,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = petrobrasYellow),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, petrobrasYellow)
            ) {
                Text("Redefinir Calibração de Íris/Face ID", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rodapé / Versão
            Text(
                text = "Petrobras VR OS - v1.0.4",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
            )
        }
    }
}