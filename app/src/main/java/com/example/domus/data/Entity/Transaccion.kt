package com.example.domus.data.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class TipoTransaccion {
    GASTO, INGRESO, TRANSFERENCIA
}

@Entity(tableName = "transacciones")
data class Transaccion(
    @PrimaryKey
    var id: String = "",

    val descripcion: String = "",
    val cantidad: Double = 0.0,
    val tipo: String = TipoTransaccion.GASTO.name,

    // Quién pagó
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    
    // A qué familia pertenece (si es nulo, es personal)
    val familiaId: String? = null,

    // Quiénes tienen que pagar (lista de IDs de usuario)
    val participantes: List<String> = emptyList(),

    @ServerTimestamp
    val fecha: Date? = null
)