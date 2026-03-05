package com.example.domus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.domus.data.Entity.Transaccion

@Database(entities = [Transaccion::class], version = 2, exportSchema = false) // VERSIÓN INCREMENTADA
@TypeConverters(Converters::class)
abstract class DB_Domus : RoomDatabase() {

    abstract fun transaccionDao(): TransaccionDao

    companion object {
        @Volatile
        private var INSTANCE: DB_Domus? = null

        fun getDatabase(context: Context): DB_Domus {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DB_Domus::class.java,
                    "domus_db"
                )
                .fallbackToDestructiveMigration() // AÑADIDA ESTRATEGIA DE MIGRACIÓN
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}