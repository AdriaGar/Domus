package com.example.domus.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domus.app.Almacen
import kotlinx.coroutines.flow.Flow

@Dao
interface AlmacenDao {
    @Query("SELECT * FROM almacenes")
    fun getAll(): Flow<List<Almacen>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(almacenes: List<Almacen>)

    @Query("DELETE FROM almacenes")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(almacen: Almacen)
}
