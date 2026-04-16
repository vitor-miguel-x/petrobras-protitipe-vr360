package com.example.petrobras_vr_prototipe.model

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class EyeAnalyzer(
    // 1. Mudamos de (Boolean) para (Int?)
    private val onEyeDetected: (Int?) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    // Usando a anotação direta do CameraX para suprimir o aviso do imageProxy.image
    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    // 3. Variável para guardar o ID
                    var faceId: Int? = null

                    for (face in faces) {
                        val leftEyeOpenProb = face.leftEyeOpenProbability
                        val rightEyeOpenProb = face.rightEyeOpenProbability

                        if (leftEyeOpenProb != null && rightEyeOpenProb != null) {
                            if (leftEyeOpenProb > 0.5f && rightEyeOpenProb > 0.5f) {
                                // 4. Pega o ID único do rosto gerado pelo ML Kit
                                faceId = face.trackingId
                                break
                            }
                        }
                    }
                    // 5. Envia o ID para a tela (ou null se não encontrar)
                    onEyeDetected(faceId)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    // Fundamental fechar o proxy para a câmera continuar processando os próximos frames
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}