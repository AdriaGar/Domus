package com.example.domus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.domus.data.Entity.Entity_Familia
import com.example.domus.data.Entity.Transaccion

@Database(entities = [Transaccion::class, Entity_Familia::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class DB_Domus : RoomDatabase() {

    abstract fun transaccionDao(): TransaccionDao
    abstract fun familiaDao(): FamiliaDao

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}