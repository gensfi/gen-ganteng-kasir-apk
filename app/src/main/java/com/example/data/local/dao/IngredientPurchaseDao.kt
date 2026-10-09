package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.IngredientPurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientPurchaseDao {
    @Query("SELECT * FROM ingredient_purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<IngredientPurchaseEntity>>

    @Query("SELECT * FROM ingredient_purchases WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getPurchasesBetween(startTime: Long, endTime: Long): List<IngredientPurchaseEntity>

    @Query("SELECT * FROM ingredient_purchases ORDER BY timestamp DESC")
    suspend fun getAllPurchasesList(): List<IngredientPurchaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: IngredientPurchaseEntity): Long

    @Delete
    suspend fun deletePurchase(purchase: IngredientPurchaseEntity)

    @Query("DELETE FROM ingredient_purchases")
    suspend fun deleteAllPurchases()
}
