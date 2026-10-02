package com.example.data.backup

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.IngredientEntity
import com.example.data.local.entity.MenuEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupRestoreManager(private val db: AppDatabase) {

    suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // 1. Users
        val users = db.userDao().getAllUsers().first()
        val usersArr = JSONArray()
        users.forEach { u ->
            usersArr.put(JSONObject().apply {
                put("id", u.id)
                put("username", u.username)
                put("name", u.name)
                put("pin", u.pin)
                put("role", u.role)
                put("createdAt", u.createdAt)
            })
        }
        root.put("users", usersArr)

        // 2. Categories
        val categories = db.categoryDao().getAllCategories().first()
        val catArr = JSONArray()
        categories.forEach { c ->
            catArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("iconName", c.iconName)
                put("colorHex", c.colorHex)
            })
        }
        root.put("categories", catArr)

        // 3. Ingredients
        val ingredients = db.ingredientDao().getAllIngredients().first()
        val ingArr = JSONArray()
        ingredients.forEach { i ->
            ingArr.put(JSONObject().apply {
                put("id", i.id)
                put("name", i.name)
                put("unit", i.unit)
                put("currentStock", i.currentStock)
                put("minStock", i.minStock)
                put("costPerUnit", i.costPerUnit)
            })
        }
        root.put("ingredients", ingArr)

        // 4. Menus
        val menus = db.menuDao().getAllMenus().first()
        val menuArr = JSONArray()
        menus.forEach { m ->
            menuArr.put(JSONObject().apply {
                put("id", m.id)
                put("categoryId", m.categoryId)
                put("name", m.name)
                put("description", m.description)
                put("price", m.price)
                put("costPrice", m.costPrice)
                put("imageUrl", m.imageUrl.orEmpty())
                put("isAvailable", m.isAvailable)
            })
        }
        root.put("menus", menuArr)

        // 5. Recipes
        val recipes = db.recipeDao().getAllRecipes().first()
        val recipeArr = JSONArray()
        recipes.forEach { r ->
            recipeArr.put(JSONObject().apply {
                put("id", r.id)
                put("menuId", r.menuId)
                put("ingredientId", r.ingredientId)
                put("amountRequired", r.amountRequired)
            })
        }
        root.put("recipes", recipeArr)

        // 6. Settings
        val settings = db.settingDao().getAllSettings().first()
        val setArr = JSONArray()
        settings.forEach { s ->
            setArr.put(JSONObject().apply {
                put("key", s.key)
                put("value", s.value)
            })
        }
        root.put("settings", setArr)

        root.put("version", 1)
        root.put("exportTimestamp", System.currentTimeMillis())

        root.toString(2)
    }

    suspend fun restoreDatabaseFromJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Restore categories
            if (root.has("categories")) {
                val catArr = root.getJSONArray("categories")
                for (i in 0 until catArr.length()) {
                    val obj = catArr.getJSONObject(i)
                    db.categoryDao().insertCategory(
                        CategoryEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            iconName = obj.optString("iconName", "Coffee"),
                            colorHex = obj.optString("colorHex", "#4A2C1D")
                        )
                    )
                }
            }

            // Restore ingredients
            if (root.has("ingredients")) {
                val ingArr = root.getJSONArray("ingredients")
                for (i in 0 until ingArr.length()) {
                    val obj = ingArr.getJSONObject(i)
                    db.ingredientDao().insertIngredient(
                        IngredientEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            unit = obj.getString("unit"),
                            currentStock = obj.getDouble("currentStock"),
                            minStock = obj.getDouble("minStock"),
                            costPerUnit = obj.optDouble("costPerUnit", 0.0)
                        )
                    )
                }
            }

            // Restore menus
            if (root.has("menus")) {
                val menuArr = root.getJSONArray("menus")
                for (i in 0 until menuArr.length()) {
                    val obj = menuArr.getJSONObject(i)
                    db.menuDao().insertMenu(
                        MenuEntity(
                            id = obj.optLong("id", 0),
                            categoryId = obj.getLong("categoryId"),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            price = obj.getDouble("price"),
                            costPrice = obj.optDouble("costPrice", 0.0),
                            imageUrl = obj.optString("imageUrl", null),
                            isAvailable = obj.optBoolean("isAvailable", true)
                        )
                    )
                }
            }

            // Restore recipes
            if (root.has("recipes")) {
                val recArr = root.getJSONArray("recipes")
                for (i in 0 until recArr.length()) {
                    val obj = recArr.getJSONObject(i)
                    db.recipeDao().insertRecipe(
                        RecipeEntity(
                            id = obj.optLong("id", 0),
                            menuId = obj.getLong("menuId"),
                            ingredientId = obj.getLong("ingredientId"),
                            amountRequired = obj.getDouble("amountRequired")
                        )
                    )
                }
            }

            // Restore settings
            if (root.has("settings")) {
                val setArr = root.getJSONArray("settings")
                for (i in 0 until setArr.length()) {
                    val obj = setArr.getJSONObject(i)
                    db.settingDao().setSetting(
                        SettingEntity(
                            key = obj.getString("key"),
                            value = obj.getString("value")
                        )
                    )
                }
            }

            Result.success("Database berhasil dipulihkan dari data cadangan.")
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membaca file cadangan: ${e.localizedMessage}"))
        }
    }
}
