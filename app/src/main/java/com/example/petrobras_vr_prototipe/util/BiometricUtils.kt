package com.example.petrobras_vr_prototipe.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// O nome do objeto DEVE ser BiometricUtils
object BiometricUtils {

    fun autenticar(
        activity: FragmentActivity,
        onSucesso: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSucesso()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            })

        // Dentro do seu BiometricUtils.kt
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticação de Íris")
            .setSubtitle("Posicione os olhos diante da câmera")
            .setNegativeButtonText("Cancelar") // <--- ADICIONE OU CORRIJA ESTA LINHA
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}