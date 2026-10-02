package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unit: String, // Gram, ML, Pcs, Sachet
    val currentStock: Double,
    val minStock: Double,
    val costPerUnit: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
