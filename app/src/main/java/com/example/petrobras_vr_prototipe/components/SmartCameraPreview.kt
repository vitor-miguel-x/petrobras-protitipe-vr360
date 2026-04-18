package com.example.petrobras_vr_prototipe.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.petrobras_vr_prototipe.model.EyeAnalyzer
// Importe o seu novo HandAnalyzer aqui
import com.example.petrobras_vr_prototipe.model.HandAnalyzer

@Composable
fun SmartCameraPreview(
    isFrontCamera: Boolean,
    onEyeDetectedState: (Int?) -> Unit,
    // NOVO: Callback para atualizar a posição do mouse e o clique
    onHandTrackingUpdate: (Offset, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            val executor = ContextCompat.getMainExecutor(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // IMPORTANTE: Unbind precisa ser total antes de qualquer nova tentativa
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    if (isFrontCamera) {
                        val analyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build().also {
                                it.setAnalyzer(executor, EyeAnalyzer { id -> onEyeDetectedState(id) })
                            }
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analyzer)
                    } else {
                        val handAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build().also {
                                it.setAnalyzer(executor, HandAnalyzer(context) { offset, isClicking ->
                                    onHandTrackingUpdate(offset, isClicking)
                                })
                            }
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, handAnalyzer)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CAMERA_ERROR", "Erro ao vincular câmera: ${e.message}")
                }
            }, executor)
        }
    )
}