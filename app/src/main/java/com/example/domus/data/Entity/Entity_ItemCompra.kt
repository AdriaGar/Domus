package com.example.domus.data.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lista_compra")
data class Entity_ItemCompra(
    @PrimaryKey val id: String = "",
    val nombre: String = "",
    var comprado: Boolean = false,
    val familiaId: String? = null,
    val usuarioId: String = ""
)
