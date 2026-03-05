package com.example.domus.data.database

import androidx.room.*
import com.example.domus.data.Entity.Entity_Familia
import kotlinx.coroutines.flow.Flow

@Dao
interface FamiliaDao {
    @Query("SELECT * FROM familia LIMIT 1")
    fun getFamilia(): Flow<Entity_Familia?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilia(familia: Entity_Familia)

    @Query("DELETE FROM familia")
    suspend fun deleteFamilia()
}