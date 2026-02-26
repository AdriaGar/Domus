package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.Entity.Entity_Usuario
import com.example.domus.data.RetrofitClient
import java.lang.Exception

class Repo_Usuario {

    private val apiService = RetrofitClient.instance

    suspend fun sendNewUser(usuario: Entity_Usuario) : Result<Boolean> {
        return try {
            val response = apiService.sendNewUser(usuario)
            if (response.isSuccessful) {
                Log.d("Repo_Usuario", "Usuario insertado correctamente")
                Result.success(true)
            } else {
                val errorMsg = "ERROR al instertar usuraio: ${response.errorBody()?.string()}"
                Log.e("Repo_Usuario", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = "Send user error: ${e.message}"
            Log.e("Repo_Usuario", errorMsg, e)
            Result.failure(Exception(errorMsg))
        }
    }

}