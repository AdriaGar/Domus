package com.example.domus.data.database

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    // --- Conversores para el tipo Date ---
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // --- Conversores para la lista de participantes ---
    @TypeConverter
    fun fromString(value: String?): List<String> {
        // Si el valor es nulo o está vacío, devuelve una lista vacía
        return value?.split(",")?.map { it.trim() } ?: emptyList()
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        // Une los elementos de la lista en un único String separado por comas
        return list?.joinToString(",") ?: ""
    }
}
