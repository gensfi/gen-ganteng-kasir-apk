package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MenuEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menus ORDER BY name ASC")
    fun getAllMenus(): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menus WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getMenusByCategory(categoryId: Long): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menus WHERE id = :id LIMIT 1")
    suspend fun getMenuById(id: Long): MenuEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(menu: MenuEntity): Long

    @Update
    suspend fun updateMenu(menu: MenuEntity)

    @Delete
    suspend fun deleteMenu(menu: MenuEntity)

    @Query("SELECT COUNT(*) FROM menus")
    suspend fun getMenuCount(): Int
}
