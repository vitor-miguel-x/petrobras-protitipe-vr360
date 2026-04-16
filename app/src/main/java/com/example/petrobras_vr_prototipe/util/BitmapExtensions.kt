package com.example.petrobras_vr_prototipe.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object ImageUtils {
    
    /**
     * Carrega um recurso de imagem aplicando downsampling (inSampleSize) para economizar RAM.
     * Ideal para imagens em drawable-nodpi.
     */
    fun decodeSampledBitmapFromResource(
        context: Context,
        resId: Int,
        reqWidth: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeResource(context.resources, resId, this)
                
                // Calcula o fator de redução
                inSampleSize = calculateInSampleSize(this, reqWidth)
                
                inJustDecodeBounds = false
                inScaled = false // Como está em nodpi, garantimos que o sistema não escale
            }
            BitmapFactory.decodeResource(context.resources, resId, options)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Erro ao decodificar recurso $resId: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int): Int {
        val width = options.outWidth
        var inSampleSize = 1
        if (width > reqWidth) {
            val halfWidth = width / 2
            while (halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

/**
 * Helper para Compose que lembra o Bitmap carregado de forma segura.
 */
@Composable
fun rememberSafeBitmap(resId: Int, targetWidth: Int = 1280): Bitmap? {
    val context = LocalContext.current
    return remember(resId) {
        ImageUtils.decodeSampledBitmapFromResource(context, resId, targetWidth)
    }
}
