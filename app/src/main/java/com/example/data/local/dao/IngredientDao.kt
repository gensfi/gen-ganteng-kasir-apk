package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    fun getAllIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun getIngredientById(id: Long): IngredientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: IngredientEntity): Long

    @Update
    suspend fun updateIngredient(ingredient: IngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: IngredientEntity)

    @Query("UPDATE ingredients SET currentStock = currentStock - :amount, updatedAt = :timestamp WHERE id = :ingredientId")
    suspend fun deductStock(ingredientId: Long, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE ingredients SET currentStock = currentStock + :amount, updatedAt = :timestamp WHERE id = :ingredientId")
    suspend fun addStock(ingredientId: Long, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM ingredients WHERE currentStock <= minStock ORDER BY currentStock ASC")
    fun getLowStockIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun getIngredientCount(): Int
}
