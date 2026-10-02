package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes WHERE menuId = :menuId")
    suspend fun getRecipesForMenu(menuId: Long): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE menuId = :menuId")
    fun getRecipesForMenuFlow(menuId: Long): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Query("DELETE FROM recipes WHERE menuId = :menuId")
    suspend fun deleteRecipesByMenuId(menuId: Long)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)
}
