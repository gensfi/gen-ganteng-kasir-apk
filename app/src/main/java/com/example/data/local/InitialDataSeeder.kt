package com.example.data.local

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.IngredientEntity
import com.example.data.local.entity.IngredientPurchaseEntity
import com.example.data.local.entity.MenuEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VoucherEntity

object InitialDataSeeder {

    suspend fun seedIfNeeded(database: AppDatabase) {
        val userCount = database.userDao().getUserCount()
        if (userCount > 0) {
            // Backfill images for existing menus if they don't have imageUrl
            backfillImagesIfMissing(database)
            seedDefaultVouchersIfNeeded(database)
            seedSamplePurchasesIfNeeded(database)
            return
        }

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
            SettingEntity("cafe_name", "Rencana Kopi"),
            SettingEntity("cafe_address", "Jl. Malioboro No. 45, Yogyakarta"),
            SettingEntity("cafe_phone", "0812-3456-7890"),
            SettingEntity("receipt_footer", "Terima kasih atas kunjungan Anda!\nFollow IG: @rencanakopi.id"),
            SettingEntity("tax_percentage", "10.0"),
            SettingEntity("service_charge_percentage", "5.0"),
            SettingEntity("printer_paper_size", "58"),
            SettingEntity("points_earn_rate", "200"),
            SettingEntity("points_redeem_rate", "1"),
            SettingEntity("google_sheets_url", ""),
            SettingEntity("google_sheets_id", ""),
            SettingEntity("google_sheets_api_key", ""),
            SettingEntity("auto_sync_sheets", "false")
        )
        defaultSettings.forEach { database.settingDao().setSetting(it) }

        // 3, 4, 5. Categories, Ingredients, Menus & Recipes
        seedSampleCategoriesAndProducts(database)
        seedDefaultVouchersIfNeeded(database)
        seedSamplePurchasesIfNeeded(database)
    }

    suspend fun resetDatabaseToFactory(
        database: AppDatabase,
        includeSampleData: Boolean = true,
        preserveAdminUser: UserEntity? = null
    ) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // Clear all tables (Semua transaksi, laporan, bahan baku, produk, voucher, member terhapus)
        database.clearAllTables()

        // 1. Users Default
        if (preserveAdminUser != null) {
            database.userDao().insertUser(
                UserEntity(
                    username = preserveAdminUser.username,
                    name = preserveAdminUser.name,
                    pin = preserveAdminUser.pin,
                    role = "ADMIN"
                )
            )
            if (preserveAdminUser.username.lowercase() != "admin") {
                database.userDao().insertUser(
                    UserEntity(
                        username = "admin",
                        name = "Owner / Manager",
                        pin = "1234",
                        role = "ADMIN"
                    )
                )
            }
        } else {
            database.userDao().insertUser(
                UserEntity(
                    username = "admin",
                    name = "Owner / Manager",
                    pin = "1234",
                    role = "ADMIN"
                )
            )
        }

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
            SettingEntity("cafe_name", "Rencana Kopi"),
            SettingEntity("cafe_address", "Jl. Malioboro No. 45, Yogyakarta"),
            SettingEntity("cafe_phone", "0812-3456-7890"),
            SettingEntity("receipt_footer", "Terima kasih atas kunjungan Anda!\nFollow IG: @rencanakopi.id"),
            SettingEntity("tax_percentage", "10.0"),
            SettingEntity("service_charge_percentage", "5.0"),
            SettingEntity("printer_paper_size", "58"),
            SettingEntity("points_earn_rate", "200"),
            SettingEntity("points_redeem_rate", "1"),
            SettingEntity("google_sheets_url", ""),
            SettingEntity("google_sheets_id", ""),
            SettingEntity("google_sheets_api_key", ""),
            SettingEntity("auto_sync_sheets", "false")
        )
        defaultSettings.forEach { database.settingDao().setSetting(it) }

        // 3. Seed Sample Data if requested
        if (includeSampleData) {
            seedSampleCategoriesAndProducts(database)
            seedDefaultVouchersIfNeeded(database)
        }
    }

    private suspend fun seedSampleCategoriesAndProducts(database: AppDatabase) {
        // Categories
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

        // Ingredients (Bahan Baku)
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

        // Menus & Recipes
        val menuEspressoId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catCoffeeId,
                name = "Espresso Single",
                description = "Single shot ekstrak biji kopi arabica premium",
                price = 15000.0,
                costPrice = 5050.0,
                imageUrl = "https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=600&q=80"
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuEspressoId, ingredientId = ingKopiId, amountRequired = 18.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuEspressoId, ingredientId = ingCupHotId, amountRequired = 1.0))

        val menuAmericanoId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catCoffeeId,
                name = "Iced Americano",
                description = "Espresso double shot segar dipadu es kristal dingin",
                price = 22000.0,
                costPrice = 5650.0,
                imageUrl = "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=600&q=80"
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuAmericanoId, ingredientId = ingKopiId, amountRequired = 20.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuAmericanoId, ingredientId = ingCupColdId, amountRequired = 1.0))

        val menuKopiArenId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catCoffeeId,
                name = "Kopi Susu Gula Aren",
                description = "Menu signature: Espresso, fresh milk, dan gula aren organik",
                price = 25000.0,
                costPrice = 8475.0,
                imageUrl = "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=600&q=80"
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuKopiArenId, ingredientId = ingKopiId, amountRequired = 18.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuKopiArenId, ingredientId = ingSusuId, amountRequired = 120.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuKopiArenId, ingredientId = ingArenId, amountRequired = 25.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuKopiArenId, ingredientId = ingCupColdId, amountRequired = 1.0))

        val menuCaramelId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catCoffeeId,
                name = "Caramel Macchiato",
                description = "Espresso creamy dengan layer susu kental & saus karamel lezat",
                price = 28000.0,
                costPrice = 10450.0,
                imageUrl = "https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=600&q=80"
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCaramelId, ingredientId = ingKopiId, amountRequired = 18.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCaramelId, ingredientId = ingSusuId, amountRequired = 140.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCaramelId, ingredientId = ingKaramelId, amountRequired = 25.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCaramelId, ingredientId = ingCupColdId, amountRequired = 1.0))

        val menuMatchaId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catNonCoffeeId,
                name = "Uji Matcha Latte",
                description = "Matcha murni khas Uji Jepang dipadu susu segar gurih",
                price = 27000.0,
                costPrice = 9650.0,
                imageUrl = "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=600&q=80"
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuMatchaId, ingredientId = ingMatchaId, amountRequired = 20.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuMatchaId, ingredientId = ingSusuId, amountRequired = 150.0))
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuMatchaId, ingredientId = ingCupColdId, amountRequired = 1.0))

        val menuCroissantId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catPastryId,
                name = "French Butter Croissant",
                description = "Croissant renyah berlapis dengan aroma butter Prancis kaya",
                price = 24000.0,
                costPrice = 8500.0,
                imageUrl = "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=600&q=80"
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuCroissantId, ingredientId = ingCroissantId, amountRequired = 1.0))

        val menuToastId = database.menuDao().insertMenu(
            MenuEntity(
                categoryId = catPastryId,
                name = "Kaya Toast Brioche",
                description = "Roti brioche panggang wangi selai srikaya pandan & butter dingin",
                price = 20000.0,
                costPrice = 4000.0,
                imageUrl = "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=600&q=80"
            )
        )
        database.recipeDao().insertRecipe(RecipeEntity(menuId = menuToastId, ingredientId = ingToastId, amountRequired = 1.0))
    }

    private suspend fun backfillImagesIfMissing(database: AppDatabase) {
        val imageMap = mapOf(
            "Espresso Single" to "https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=600&q=80",
            "Iced Americano" to "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=600&q=80",
            "Kopi Susu Gula Aren" to "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=600&q=80",
            "Caramel Macchiato" to "https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=600&q=80",
            "Uji Matcha Latte" to "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=600&q=80",
            "French Butter Croissant" to "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=600&q=80",
            "Kaya Toast Brioche" to "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=600&q=80"
        )

        // Query all menus via database query
        val menus = database.menuDao().getMenuCount()
        if (menus > 0) {
            imageMap.forEach { (name, url) ->
                // Check if menu with this name has null imageUrl
                // Using raw update if needed
                database.openHelper.writableDatabase.execSQL(
                    "UPDATE menus SET imageUrl = ? WHERE name = ? AND (imageUrl IS NULL OR imageUrl = '')",
                    arrayOf(url, name)
                )
            }
        }
    }

    suspend fun seedDefaultVouchersIfNeeded(database: AppDatabase) {
        val existing = database.voucherDao().getVoucherByCode("PROMO20")
        if (existing == null) {
            val defaultVouchers = listOf(
                VoucherEntity(
                    code = "PROMO20",
                    discountType = "PERCENT",
                    discountValue = 20.0,
                    minPurchase = 25000.0,
                    quota = 100,
                    usedCount = 0,
                    expiryDate = System.currentTimeMillis() + (365L * 24 * 3600 * 1000),
                    isActive = true
                ),
                VoucherEntity(
                    code = "KOPI10RB",
                    discountType = "NOMINAL",
                    discountValue = 10000.0,
                    minPurchase = 30000.0,
                    quota = 50,
                    usedCount = 0,
                    expiryDate = System.currentTimeMillis() + (365L * 24 * 3600 * 1000),
                    isActive = true
                ),
                VoucherEntity(
                    code = "HEMAT5",
                    discountType = "PERCENT",
                    discountValue = 5.0,
                    minPurchase = 0.0,
                    quota = 500,
                    usedCount = 0,
                    expiryDate = System.currentTimeMillis() + (365L * 24 * 3600 * 1000),
                    isActive = true
                )
            )
            defaultVouchers.forEach { database.voucherDao().insertVoucher(it) }
        }
    }

    suspend fun seedSamplePurchasesIfNeeded(database: AppDatabase) {
        val existing = database.ingredientPurchaseDao().getAllPurchasesList()
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            val purchases = listOf(
                IngredientPurchaseEntity(
                    ingredientId = 1,
                    ingredientName = "Biji Kopi House Blend",
                    quantity = 2000.0,
                    unit = "Gram",
                    costPerUnit = 120.0,
                    totalCost = 240000.0,
                    supplierName = "Roastery Nusantara",
                    notes = "Restock mingguan biji kopi",
                    cashierName = "Owner / Manager",
                    timestamp = now - (2 * 86400 * 1000L)
                ),
                IngredientPurchaseEntity(
                    ingredientId = 2,
                    ingredientName = "Susu Fresh Milk Pasteurisasi",
                    quantity = 5000.0,
                    unit = "ML",
                    costPerUnit = 22.0,
                    totalCost = 110000.0,
                    supplierName = "Dairy Freshindo",
                    notes = "Pengadaan susu segar",
                    cashierName = "Kasir 1",
                    timestamp = now - (1 * 86400 * 1000L)
                ),
                IngredientPurchaseEntity(
                    ingredientId = 3,
                    ingredientName = "Gula Aren Organik Cair",
                    quantity = 1000.0,
                    unit = "ML",
                    costPerUnit = 45.0,
                    totalCost = 45000.0,
                    supplierName = "Aren Organik Kulonprogo",
                    notes = "Restock sirup aren",
                    cashierName = "Kasir 1",
                    timestamp = now - (4 * 3600 * 1000L)
                )
            )
            purchases.forEach { database.ingredientPurchaseDao().insertPurchase(it) }
        }
    }
}
