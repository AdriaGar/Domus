package com.example.domus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domus.data.Entity.Entity_ItemCompra
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemCompraDao {

    @Query("SELECT * FROM lista_compra")
    fun getAll(): Flow<List<Entity_ItemCompra>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Entity_ItemCompra>)

    @Query("DELETE FROM lista_compra")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(item: Entity_ItemCompra)
}
