package com.example.domus.data.api

import com.example.domus.data.Entity.Entity_Usuario
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface Api_Usuario {

    @POST("registrar")
    suspend fun sendNewUser(@Body usuario: Entity_Usuario): Response<Entity_Usuario>

    @POST("login")
    suspend fun login(@Body usuario: Entity_Usuario): Response<Entity_Usuario>

}
