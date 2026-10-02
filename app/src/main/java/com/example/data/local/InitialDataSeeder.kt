package com.example.data.local

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.IngredientEntity
import com.example.data.local.entity.MenuEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.UserEntity

object InitialDataSeeder {

    suspend fun seedIfNeeded(database: AppDatabase) {
        val userCount = database.userDao().getUserCount()
        if (userCount > 0) return

        // 1. Default Users (Admin & Kasir)
        database.userDao().insertUser(
            UserEntity(
                username = "admin",
                name = "Owner / Manager",
                pin = "1234",
                role = "ADMIN"
            )
        )
        database.userDao().insertUser(
            UserEntity(
                username = "kasir",
                name = "Kasir 1",
                pin = "1111",
                role = "KASIR"
            )
        )

        // 2. Default Cafe Settings
        val defaultSettings = listOf(
            SettingEntity("cafe_name", "Kopi Kenangan Senja"),
            SettingEntity("cafe_address", "Jl. Malioboro No. 45, Yogyakarta"),
            SettingEntity("cafe_phone", "0812-3456-7890"),
            SettingEntity("receipt_footer", "Terima kasih atas kunjungan Anda!\nFollow IG: @kopisenja.cafe"),
            SettingEntity("tax_percentage", "10.0"),
            SettingEntity("service_charge_percentage", "5.0"),
            SettingEntity("printer_paper_size", "58"),
            SettingEntity("google_sheets_url", ""),
            SettingEntity("google_sheets_id", ""),
            SettingEntity("google_sheets_api_key", ""),
            SettingEntity("auto_sync_sheets", "false")
        )
        defaultSettings.forEach { database.settingDao().setSetting(it) }

        // 3. Categories
        val catCoffeeId = database.categoryDao().insertCategory(
            CategoryEntity(name = "Espresso & Coffee", iconName = "Coffee", colorHex = "#4A2C1D")
        )
        val catNonCoffeeId = database.categoryDao().insertCategory(
            CategoryEntity(name = "Non-Coffee & Tea", iconName = "Tea", colorHex = "#2E7D32")
        )
        val catPastryId = database.categoryDao().insertCategory(
            CategoryEntity(name = "Bakery & Pastry", iconName = "Bakery", colorHex = "#D49B5B")
        )
        val catFoodId = database.categoryDao().insertCategory(
            CategoryEntity(name = "Snack & Meals", iconName = "Food", colorHex = "#C62828")
        )

        // 4. Ingredients (Bahan Baku)
        val ingKopiId = database.ingredientDao().insertIngredient(
            IngredientEntity(name = "Biji Kopi Arabica", unit = "Gram", currentStock = 2500.0, minStock = 500.0, costPerUnit = 250.0)
        )
        val ingSusuId = database.ingredientDao().insertIngredient(
            IngredientEntity(name = "Susu Fresh Milk", unit = "ML", currentStock = 8000.0, minStock = 1500.0, costPerUnit = 20.0)
        )
        val ingArenId = database.ingredientDao().insertIngredient(
            IngredientEntity(name = "Gula Aren Cair", unit = "ML", currentStock = 3000.0, minStock = 500.0, costPerUnit = 35.0)
        )
        val ingKaramelId = database.ingredientDao().insertIngredient(
            IngredientEntity(name = "Sirup Karamel", unit = "ML", currentStock = 1200.0, minStock = 300.0, costPerUnit = 90.0)
        )
        val ingMatchaId = database.ingredientDao().insertIngredient(
            IngredientEntity(name = "Bubuk Matcha Uji", unit = "Gram", currentStock = 800.0, minStock = 200.0, costPerUnit = 300.0)
        )
        val ingCupColdId = database.ingredientDao().insertIngredient(
            IngredientEntity(name = "Cup Cold 16oz + Straw", unit = "Pcs", currentStock = 250.0, minStock = 50.0, costPerUnit = 650.0)
        )
        val ingCupHotId = database.ingredientDao().insertIngredient(
            IngredientEntity(name = "Cup Hot 8oz + Lid", unit = "Pcs", currentStock = 180.0, minStock = 40.0, costPerUnit = 550.0)
        )
        val ingCroissantId = database.ingredientDao().insertIngredient(
            IngredientEntity(name = "Adonan Butter Croissant", unit = "Pcs", currentStock = 35.0, minStock = 10.0, costPerUnit = 8500.0)
        )
        val ingToastId = database.ingredientDao().insertIngredient(
            IngredientEntity(name = "Roti Brioche Toast", unit = "Pcs", currentStock = 40.0, minStock = 10.0, costPerUnit = 4000.0)
        )

        // 5. Menus & Recipes
        // Menu 1: Espresso Single Hot
        val menuEspressoId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catCoffeeId,
                name = "Espresso Single",
                description = "Single shot ekstrak biji kopi arabica premium",
                price = 15000.0,
                costPrice = 5050.0
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuEspressoId, ingredientId = ingKopiId, amountRequired = 18.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuEspressoId, ingredientId = ingCupHotId, amountRequired = 1.0))

        // Menu 2: Americano Iced
        val menuAmericanoId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catCoffeeId,
                name = "Iced Americano",
                description = "Espresso double shot segar dipadu es kristal dingin",
                price = 22000.0,
                costPrice = 5650.0
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuAmericanoId, ingredientId = ingKopiId, amountRequired = 20.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuAmericanoId, ingredientId = ingCupColdId, amountRequired = 1.0))

        // Menu 3: Kopi Susu Senja (Aren)
        val menuKopiArenId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catCoffeeId,
                name = "Kopi Susu Gula Aren",
                description = "Menu signature: Espresso, fresh milk, dan gula aren organik",
                price = 25000.0,
                costPrice = 8475.0
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuKopiArenId, ingredientId = ingKopiId, amountRequired = 18.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuKopiArenId, ingredientId = ingSusuId, amountRequired = 120.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuKopiArenId, ingredientId = ingArenId, amountRequired = 25.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuKopiArenId, ingredientId = ingCupColdId, amountRequired = 1.0))

        // Menu 4: Caramel Macchiato
        val menuCaramelId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catCoffeeId,
                name = "Caramel Macchiato",
                description = "Espresso creamy dengan layer susu kental & saus karamel lezat",
                price = 28000.0,
                costPrice = 10450.0
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCaramelId, ingredientId = ingKopiId, amountRequired = 18.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCaramelId, ingredientId = ingSusuId, amountRequired = 140.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCaramelId, ingredientId = ingKaramelId, amountRequired = 25.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCaramelId, ingredientId = ingCupColdId, amountRequired = 1.0))

        // Menu 5: Uji Matcha Latte
        val menuMatchaId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catNonCoffeeId,
                name = "Uji Matcha Latte",
                description = "Matcha murni khas Uji Jepang dipadu susu segar gurih",
                price = 27000.0,
                costPrice = 9650.0
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuMatchaId, ingredientId = ingMatchaId, amountRequired = 20.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuMatchaId, ingredientId = ingSusuId, amountRequired = 150.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuMatchaId, ingredientId = ingCupColdId, amountRequired = 1.0))

        // Menu 6: Butter Croissant
        val menuCroissantId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catPastryId,
                name = "French Butter Croissant",
                description = "Croissant renyah berlapis dengan aroma butter Prancis kaya",
                price = 24000.0,
                costPrice = 8500.0
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCroissantId, ingredientId = ingCroissantId, amountRequired = 1.0))

        // Menu 7: Kaya Toast Brioche
        val menuToastId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catPastryId,
                name = "Kaya Toast Brioche",
                description = "Roti brioche panggang wangi selai srikaya pandan & butter dingin",
                price = 20000.0,
                costPrice = 4000.0
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuToastId, ingredientId = ingToastId, amountRequired = 1.0))
    }
}
