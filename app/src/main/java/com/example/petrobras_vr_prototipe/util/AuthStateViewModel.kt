package com.example.petrobras_vr_prototipe.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import java.util.UUID

class AuthViewModel : ViewModel() {

    private var sharedPrefs: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPrefs == null) {
            sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        }
    }

    // CORREÇÃO 1: A função não pede mais o "faceId: Int"
    fun getPersistentHash(faceId: Int): String {
        val prefs = sharedPrefs ?: return "LOADING..."

        // Agora usamos uma chave fixa "global_user_hash"
        val existingHash = prefs.getString("global_user_hash", null)

        if (existingHash != null) {
            return existingHash
        }

        // Se nunca foi gerado, cria um e salva para sempre
        val newHash = generateFakeArgon2()
        prefs.edit().putString("global_user_hash", newHash).apply()

        return newHash
    }

    // CORREÇÃO 2: A função geradora também não pede mais o "id: Int"
    private fun generateFakeArgon2(): String {
        // Gera um Argon2 fixo simulando um usuário logado no dispositivo
        val salt = UUID.randomUUID().toString().take(8)
        val hash = "funcionario_petrobras_01".hashCode().toString(16)
        return "\$argon2id\$v=19\$m=65536,t=3,p=4\$$salt\$$hash"
    }
}