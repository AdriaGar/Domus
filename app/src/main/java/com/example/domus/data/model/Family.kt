package com.example.domus.data.model

data class Family(
    val id: String = "",
    val name: String = "",
    val adminId: String = "",
    val creatorId: String = "",
    val joinCode: String = "",
    val codeCreatedAt: Long = 0L, // Timestamp in milliseconds
    val members: List<String> = emptyList(),
    val pendingMembers: List<String> = emptyList()
)