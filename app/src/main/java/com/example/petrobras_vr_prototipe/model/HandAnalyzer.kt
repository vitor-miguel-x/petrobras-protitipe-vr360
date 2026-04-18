package com.example.petrobras_vr_prototipe.model

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import kotlin.math.sqrt

class HandAnalyzer(
    context: Context,
    private val onHandResult: (Offset, Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private var handLandmarker: HandLandmarker? = null

    init {
        // 1. Configuramos o caminho do modelo nos assets
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        // 2. Configuramos as opções de detecção
        val options = HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setResultListener { result, _ ->
                if (result.landmarks().isNotEmpty()) {
                    val handLandmarks = result.landmarks()[0]

                    val indexTip = handLandmarks[8]
                    val thumbTip = handLandmarks[4]

                    val cursorX = indexTip.x()
                    val cursorY = indexTip.y()

                    val dx = indexTip.x() - thumbTip.x()
                    val dy = indexTip.y() - thumbTip.y()
                    val dz = indexTip.z() - thumbTip.z()
                    val distance = sqrt((dx * dx + dy * dy + dz * dz).toDouble())

                    val isClicking = distance < 0.04

                    onHandResult(Offset(cursorX, cursorY), isClicking)
                }
            }
            .setErrorListener { error ->
                error.printStackTrace()
            }
            .build()

        // 3. CORREÇÃO: Criamos o landmarker apenas com o contexto e as opções
        // O caminho do asset já está embutido no objeto 'options'
        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()

        handLandmarker?.detectAsync(mpImage, imageProxy.imageInfo.timestamp)

        imageProxy.close()
    }
}