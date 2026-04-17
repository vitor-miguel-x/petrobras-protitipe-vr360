package com.example.petrobras_vr_prototipe.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsOverlay(
    onClose: () -> Unit,
    onResetBiometrics: () -> Unit
) {
    // Variáveis de estado simuladas para a UI
    var voiceEnabled by remember { mutableStateOf(true) }
    var arOpacity by remember { mutableStateOf(0.8f) }

    Box(
        modifier = Modifier.graphicsLayer(
            rotationY = 20f,
            rotationX = -10f,
            cameraDistance = 12f
        )
            .height(225.dp)
            .width(330.dp)
            .background(Color.Black.copy(alpha = 0.4f)), // Fundo escuro para destacar o painel
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(450.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E1E).copy(alpha = 0.9f)) // Estilo Painel VR
                .border(1.dp, Color(0xFF008A52), RoundedCornerShape(16.dp)) // Borda verde Petrobras
                .padding(24.dp)
        ) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = "Config", tint = Color(0xFF008A52))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configurações do Sistema",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 16.dp))

            // Seção 1: IA e Áudio
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = "Mic", tint = Color.LightGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Assistente Brás (IA)", color = Color.LightGray, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Reconhecimento de Voz Contínuo", color = Color.White, fontSize = 14.sp)
                Switch(
                    checked = voiceEnabled,
                    onCheckedChange = { voiceEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF008A52))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seção 2: Interface AR
            Text(text = "Opacidade da Interface AR", color = Color.White, fontSize = 14.sp)
            Slider(
                value = arOpacity,
                onValueChange = { arOpacity = it },
                valueRange = 0.2f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF008A52),
                    activeTrackColor = Color(0xFF008A52)
                )
            )

            Divider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 16.dp))

            // Seção 3: Segurança
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = "Security", tint = Color.LightGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Segurança Operacional", color = Color.LightGray, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onResetBiometrics,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFA600)), // Amarelo Petrobras
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA600))
            ) {
                Text("Redefinir Calibração de Íris/Face ID")
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