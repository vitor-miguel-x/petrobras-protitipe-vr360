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

    // Removido o parâmetro faceId da assinatura
    fun getPersistentHash(): String {
        val prefs = sharedPrefs ?: return "LOADING..."

        val existingHash = prefs.getString("global_user_hash", null)

        if (existingHash != null) {
            return existingHash
        }

        val newHash = generateFakeArgon2()
        prefs.edit().putString("global_user_hash", newHash).apply()

        return newHash
    }

    private fun generateFakeArgon2(): String {
        val salt = UUID.randomUUID().toString().take(8)
        val hash = "funcionario_petrobras_01".hashCode().toString(16)
        return "\$argon2id\$v=19\$m=65536,t=3,p=4\$$salt\$$hash"
    }
}