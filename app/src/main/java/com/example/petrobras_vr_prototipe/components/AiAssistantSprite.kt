package com.example.petrobras_vr_prototipe.components

import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.petrobras_vr_prototipe.R

@Composable
fun AiAssistantSprite(texto: String, modifier: Modifier = Modifier) {

    // Animação de entrada e saída (só aparece quando a IA estiver falando algo)
    AnimatedVisibility(
        visible = texto.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier // Recebe o fillMaxSize() vindo do CameraScreen
    ) {
        val context = LocalContext.current

        // Configuração obrigatória do Coil para ele conseguir decodificar GIFs
        val imageLoader = ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        // Carrega e exibe o seu arquivo bras_falando.gif
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.drawable.bras_falando)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "IA Sprite Animado",
            contentScale = ContentScale.Crop, // Faz o GIF ocupar toda a área (cortando as sobras em vez de encolher)
            modifier = Modifier // Usa o tamanho total disponível
        )
    }
}