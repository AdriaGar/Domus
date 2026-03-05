package com.example.domus.data.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "familia")
data class Entity_Familia(
    @PrimaryKey val id: String,
    val name: String,
    val adminId: String,
    val creatorId: String,
    val joinCode: String,
    val codeCreatedAt: Long,
    val members: List<String>,
    val pendingMembers: List<String>
)