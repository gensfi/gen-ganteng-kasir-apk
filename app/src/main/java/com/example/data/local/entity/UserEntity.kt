package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val name: String,
    val pin: String, // 4-6 digit PIN
    val role: String, // "ADMIN" or "KASIR"
    val createdAt: Long = System.currentTimeMillis()
)
