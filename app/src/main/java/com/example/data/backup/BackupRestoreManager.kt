package com.example.data.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.IngredientEntity
import com.example.data.local.entity.MenuEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VoucherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupFileInfo(
    val file: File,
    val filename: String,
    val timestamp: Long,
    val sizeFormatted: String,
    val isAuto: Boolean
)

class BackupRestoreManager(private val db: AppDatabase) {

    companion object {
        const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        const val KEY_LAST_AUTO_BACKUP_TIME = "last_auto_backup_timestamp"
    }

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

        // 3. Ingredients (Inventory)
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

        // 7. Vouchers
        val vouchers = db.voucherDao().getAllVouchers().first()
        val vArr = JSONArray()
        vouchers.forEach { v ->
            vArr.put(JSONObject().apply {
                put("code", v.code)
                put("discountType", v.discountType)
                put("discountValue", v.discountValue)
                put("minPurchase", v.minPurchase)
                put("quota", v.quota)
                put("usedCount", v.usedCount)
                put("expiryDate", v.expiryDate)
                put("isActive", v.isActive)
            })
        }
        root.put("vouchers", vArr)

        // 8. Transactions & Items
        val transactions = db.transactionDao().getAllTransactions().first()
        val trxArr = JSONArray()
        transactions.forEach { t ->
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("cashierId", t.cashierId)
                put("cashierName", t.cashierName)
                put("customerName", t.customerName.orEmpty())
                put("customerPhone", t.customerPhone.orEmpty())
                put("subtotal", t.subtotal)
                put("totalAmount", t.totalAmount)
                put("paymentMethod", t.paymentMethod)
                put("cashGiven", t.cashGiven)
                put("changeAmount", t.changeAmount)
                put("taxAmount", t.taxAmount)
                put("serviceChargeAmount", t.serviceChargeAmount)
                put("discountAmount", t.discountAmount)
                put("notes", t.notes.orEmpty())
                put("timestamp", t.timestamp)
                put("isSyncedToSheets", t.isSyncedToSheets)
            }
            trxArr.put(tObj)
        }
        root.put("transactions", trxArr)

        val trxItems = db.transactionDao().getAllTransactionItems()
        val itemsArr = JSONArray()
        trxItems.forEach { item ->
            itemsArr.put(JSONObject().apply {
                put("id", item.id)
                put("transactionId", item.transactionId)
                put("menuId", item.menuId)
                put("menuName", item.menuName)
                put("quantity", item.quantity)
                put("price", item.price)
                put("costPrice", item.costPrice)
                put("itemNotes", item.itemNotes.orEmpty())
            })
        }
        root.put("transactionItems", itemsArr)

        root.put("version", 2)
        root.put("appName", "Rencana Kopi POS")
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

            // Restore vouchers
            if (root.has("vouchers")) {
                val vArr = root.getJSONArray("vouchers")
                for (i in 0 until vArr.length()) {
                    val obj = vArr.getJSONObject(i)
                    db.voucherDao().insertVoucher(
                        VoucherEntity(
                            code = obj.getString("code"),
                            discountType = obj.optString("discountType", "PERCENT"),
                            discountValue = obj.optDouble("discountValue", 0.0),
                            minPurchase = obj.optDouble("minPurchase", 0.0),
                            quota = obj.optInt("quota", -1),
                            usedCount = obj.optInt("usedCount", 0),
                            expiryDate = obj.optLong("expiryDate", obj.optLong("validUntil", System.currentTimeMillis() + 86400000L * 30)),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
            }

            // Restore transactions
            if (root.has("transactions")) {
                val trxArr = root.getJSONArray("transactions")
                for (i in 0 until trxArr.length()) {
                    val obj = trxArr.getJSONObject(i)
                    db.transactionDao().insertTransaction(
                        TransactionEntity(
                            id = obj.getString("id"),
                            cashierId = obj.optLong("cashierId", 1),
                            cashierName = obj.optString("cashierName", "Kasir"),
                            customerName = obj.optString("customerName").takeIf { it.isNotBlank() },
                            customerPhone = obj.optString("customerPhone").takeIf { it.isNotBlank() },
                            subtotal = obj.optDouble("subtotal", obj.getDouble("totalAmount")),
                            taxAmount = obj.optDouble("taxAmount", 0.0),
                            serviceChargeAmount = obj.optDouble("serviceChargeAmount", obj.optDouble("serviceAmount", 0.0)),
                            discountAmount = obj.optDouble("discountAmount", 0.0),
                            totalAmount = obj.getDouble("totalAmount"),
                            paymentMethod = obj.optString("paymentMethod", "CASH"),
                            cashGiven = obj.optDouble("cashGiven", obj.getDouble("totalAmount")),
                            changeAmount = obj.optDouble("changeAmount", 0.0),
                            notes = (obj.optString("notes").ifBlank { obj.optString("orderNotes") }).takeIf { it.isNotBlank() },
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isSyncedToSheets = obj.optBoolean("isSyncedToSheets", false)
                        )
                    )
                }
            }

            // Restore transaction items
            if (root.has("transactionItems")) {
                val itemsArr = root.getJSONArray("transactionItems")
                val list = mutableListOf<TransactionItemEntity>()
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.getJSONObject(i)
                    list.add(
                        TransactionItemEntity(
                            id = obj.optLong("id", 0),
                            transactionId = obj.getString("transactionId"),
                            menuId = obj.getLong("menuId"),
                            menuName = obj.getString("menuName"),
                            price = obj.optDouble("price", obj.optDouble("pricePerUnit", 0.0)),
                            costPrice = obj.optDouble("costPrice", 0.0),
                            quantity = obj.getInt("quantity"),
                            itemNotes = obj.optString("itemNotes").takeIf { it.isNotBlank() }
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    db.transactionDao().insertTransactionItems(list)
                }
            }

            Result.success("Seluruh data bisnis, inventaris & transaksi berhasil dipulihkan.")
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membaca atau memulihkan file cadangan: ${e.localizedMessage}"))
        }
    }

    private fun getBackupDir(context: Context): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun performAutoBackup(context: Context): File = withContext(Dispatchers.IO) {
        val json = exportDatabaseToJson()
        val dir = getBackupDir(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("id", "ID")).format(Date())
        val file = File(dir, "auto_backup_$timeStamp.json")
        FileOutputStream(file).use { it.write(json.toByteArray(Charsets.UTF_8)) }

        // Also update latest snapshot
        val latestFile = File(dir, "latest_auto_backup.json")
        FileOutputStream(latestFile).use { it.write(json.toByteArray(Charsets.UTF_8)) }

        // Save last auto backup timestamp in settings
        val now = System.currentTimeMillis()
        db.settingDao().setSetting(SettingEntity(KEY_LAST_AUTO_BACKUP_TIME, now.toString()))

        // Rotate backups: keep max 10 auto backups
        val autoBackups = dir.listFiles { f -> f.name.startsWith("auto_backup_") && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

        if (autoBackups.size > 10) {
            autoBackups.drop(10).forEach { it.delete() }
        }

        file
    }

    suspend fun performManualBackup(context: Context): File = withContext(Dispatchers.IO) {
        val json = exportDatabaseToJson()
        val dir = getBackupDir(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("id", "ID")).format(Date())
        val file = File(dir, "manual_backup_$timeStamp.json")
        FileOutputStream(file).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        file
    }

    suspend fun listBackups(context: Context): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        val dir = getBackupDir(context)
        val files = dir.listFiles { f -> f.name.endsWith(".json") && f.name != "latest_auto_backup.json" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

        files.map { f ->
            val sizeKb = (f.length() / 1024.0)
            val sizeStr = if (sizeKb >= 1024) String.format(Locale("id", "ID"), "%.1f MB", sizeKb / 1024.0)
                          else String.format(Locale("id", "ID"), "%.1f KB", sizeKb)
            BackupFileInfo(
                file = f,
                filename = f.name,
                timestamp = f.lastModified(),
                sizeFormatted = sizeStr,
                isAuto = f.name.startsWith("auto_backup_")
            )
        }
    }

    suspend fun restoreFromFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = file.readText(Charsets.UTF_8)
            restoreDatabaseFromJson(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup Data Rencana Kopi POS (${file.name})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Bagikan / Simpan File Cadangan")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    suspend fun isAutoBackupEnabled(): Boolean = withContext(Dispatchers.IO) {
        val setting = db.settingDao().getSetting(KEY_AUTO_BACKUP_ENABLED)
        setting?.value?.toBooleanStrictOrNull() ?: true // Default enabled
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        db.settingDao().setSetting(SettingEntity(KEY_AUTO_BACKUP_ENABLED, enabled.toString()))
    }

    suspend fun getLastAutoBackupTime(): Long = withContext(Dispatchers.IO) {
        val setting = db.settingDao().getSetting(KEY_LAST_AUTO_BACKUP_TIME)
        setting?.value?.toLongOrNull() ?: 0L
    }
}
