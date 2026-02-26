package com.example.domus.data.Entity

import com.google.gson.annotations.SerializedName

data class Entity_Usuario (
    @SerializedName("Nombre") val nombre: String,
    @SerializedName("Apellidos") val apellidos: String,
    @SerializedName("Email") val email: String,
    @SerializedName("Contraseña") val password: String,
)