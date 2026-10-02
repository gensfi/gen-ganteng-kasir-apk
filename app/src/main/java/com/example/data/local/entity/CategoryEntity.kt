package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "Coffee", // Coffee, Tea, Bakery, Food, Snack, IceCream
    val colorHex: String = "#4A2C1D"
)
