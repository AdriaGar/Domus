package com.example.domus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domus.data.Entity.Transaccion
import kotlinx.coroutines.flow.Flow

@Dao
interface TransaccionDao {

    /**
     * Devuelve todas las transacciones de la base de datos, ordenadas por fecha descendente.
     * Usamos un Flow para que la UI se actualice automáticamente cuando los datos cambien.
     */
    @Query("SELECT * FROM transacciones ORDER BY fecha DESC")
    fun getAll(): Flow<List<Transaccion>>

    /**
     * Inserta una lista de transacciones. Si una transacción ya existe (mismo id),
     * la reemplaza. Esto es clave para la sincronización.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transacciones: List<Transaccion>)

    /**
     * Borra todas las transacciones de la tabla. Útil para refrescar los datos
     * desde Firestore.
     */
    @Query("DELETE FROM transacciones")
    suspend fun deleteAll()
}
