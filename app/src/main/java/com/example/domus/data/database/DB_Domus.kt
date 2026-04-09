package com.example.domus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.domus.data.Entity.Entity_Familia
import com.example.domus.data.Entity.Transaccion
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.app.Producto
import com.example.domus.app.Almacen

@Database(entities = [Transaccion::class, Entity_Familia::class, Entity_ItemCompra::class, Producto::class, Almacen::class], version = 12, exportSchema = false)
@TypeConverters(Converters::class)
abstract class DB_Domus : RoomDatabase() {

    abstract fun transaccionDao(): TransaccionDao
    abstract fun familiaDao(): FamiliaDao
    abstract fun itemCompraDao(): ItemCompraDao
    abstract fun stockCocinaDao(): StockCocinaDao
    abstract fun almacenDao(): AlmacenDao

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
