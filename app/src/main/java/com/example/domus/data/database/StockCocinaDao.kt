package com.example.domus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domus.app.Producto
import kotlinx.coroutines.flow.Flow

@Dao
interface StockCocinaDao {
    @Query("SELECT * FROM stock_cocina")
    fun getAll(): Flow<List<Producto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productos: List<Producto>)

    @Query("DELETE FROM stock_cocina")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(producto: Producto)
}
