package com.example.petrobras_vr_prototipe.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // Import essencial para o viewModel() funcionar
import com.example.petrobras_vr_prototipe.util.AuthState
import com.example.petrobras_vr_prototipe.viewmodel.AuthViewModel
import kotlinx.coroutines.delay


@Composable
fun IrisAuthOverlay(
    detectedFaceId: Int?,
    viewModel: AuthViewModel = viewModel(), // Correção 1: Recebendo a ViewModel corretamente
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var authState by remember { mutableStateOf(AuthState.SEARCHING) }
    var currentGeneratedCode by remember { mutableStateOf("") }

    // Inicializa o banco de dados local
    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    LaunchedEffect(detectedFaceId) {
        if (detectedFaceId != null) {
            // Tente passar o valor diretamente sem o nome "faceId =" caso o erro persista
            val savedHash = viewModel.getPersistentHash(faceId = detectedFaceId!!)

            // Para dar efeito de VR, fazemos o "Scanning" apenas na primeira vez
            if (currentGeneratedCode != savedHash) {
                authState = AuthState.SCANNING
                delay(2500) // Tempo de "leitura"
                currentGeneratedCode = savedHash
            }

            authState = AuthState.SUCCESS

            // Correção 3: Espera um pouco para o usuário ver o "Vezinho verde" e muda a câmera!
            delay(1500)
            onAuthSuccess()

        } else {
            authState = AuthState.SEARCHING
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (authState) {
                AuthState.SEARCHING -> {
                    Text("Aguardando detecção...", color = Color.Yellow, fontSize = 12.sp)
                }
                AuthState.SCANNING -> {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("Criptografando Íris (Argon2)...", color = Color.White, fontSize = 12.sp)
                }
                AuthState.SUCCESS -> {
                    Column {
                        Text("IDENTIDADE VERIFICADA ✓", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        // Mostra o hash encurtado ou completo
                        Text(
                            text = currentGeneratedCode,
                            color = Color.Green.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 10.sp
                        )
                    }
                }
                else -> {}
            }
        }
    }
}