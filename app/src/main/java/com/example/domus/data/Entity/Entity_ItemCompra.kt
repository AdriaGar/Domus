package com.example.domus.data.Entity

import androidx.room.Entity

@Entity
data class Entity_ItemCompra(
    val nombre: String,
    var comprado: Boolean
)
