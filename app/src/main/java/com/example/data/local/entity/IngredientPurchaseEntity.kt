package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredient_purchases")
data class IngredientPurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ingredientId: Long,
    val ingredientName: String,
    val quantity: Double,
    val unit: String,
    val costPerUnit: Double,
    val totalCost: Double,
    val supplierName: String = "",
    val notes: String = "",
    val cashierName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
