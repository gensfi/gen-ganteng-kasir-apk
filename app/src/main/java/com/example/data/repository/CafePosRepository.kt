package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.IngredientEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.MenuEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VoucherEntity
import com.example.data.remote.GoogleSheetsSyncService
import com.example.data.remote.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CartItem(
    val menu: MenuEntity,
    val quantity: Int,
    val notes: String = "",
    val availableStockEstimate: Int = 999
)

data class StockValidationResult(
    val isSufficient: Boolean,
    val insufficientIngredients: List<String> = emptyList()
)

data class ReportSummary(
    val totalRevenue: Double,
    val totalNetProfit: Double,
    val transactionCount: Int,
    val topSellingItems: List<Pair<String, Int>>,
    val ingredientUsageSummary: List<Pair<String, String>>
)

class CafePosRepository(
    private val database: AppDatabase,
    private val sheetsSyncService: GoogleSheetsSyncService
) {
    private val userDao = database.userDao()
    private val categoryDao = database.categoryDao()
    private val menuDao = database.menuDao()
    private val ingredientDao = database.ingredientDao()
    private val recipeDao = database.recipeDao()
    private val transactionDao = database.transactionDao()
    private val settingDao = database.settingDao()
    private val voucherDao = database.voucherDao()
    private val memberDao = database.memberDao()

    // --- Member & Loyalty Points ---
    fun getAllMembers(): Flow<List<MemberEntity>> = memberDao.getAllMembers()
    suspend fun getMemberById(id: Long): MemberEntity? = memberDao.getMemberById(id)
    suspend fun getMemberByPhone(phone: String): MemberEntity? = memberDao.getMemberByPhone(phone.trim())

    suspend fun getMemberByPhoneFlexible(phone: String): MemberEntity? {
        val raw = phone.trim()
        if (raw.isBlank()) return null
        val direct = memberDao.getMemberByPhone(raw)
        if (direct != null) return direct

        val digits = raw.filter { it.isDigit() }
        if (digits.length < 5) return null

        val variants = linkedSetOf<String>()
        variants.add(digits)
        if (digits.startsWith("08")) {
            variants.add("628" + digits.substring(2))
            variants.add("+628" + digits.substring(2))
            variants.add(digits.substring(1))
        } else if (digits.startsWith("628")) {
            variants.add("08" + digits.substring(3))
            variants.add("+" + digits)
            variants.add(digits.substring(2))
        } else if (digits.startsWith("8")) {
            variants.add("0" + digits)
            variants.add("62" + digits)
            variants.add("+62" + digits)
        }

        for (v in variants) {
            val found = memberDao.getMemberByPhone(v)
            if (found != null) return found
        }
        return null
    }

    fun searchMembers(query: String): Flow<List<MemberEntity>> = memberDao.searchMembers(query.trim())
    suspend fun insertMember(member: MemberEntity): Long = memberDao.insertMember(member)
    suspend fun updateMember(member: MemberEntity) = memberDao.updateMember(member)
    suspend fun deleteMember(member: MemberEntity) = memberDao.deleteMember(member)
    suspend fun updateMemberPoints(memberId: Long, points: Int) = memberDao.updatePoints(memberId, points)
    suspend fun addMemberPoints(memberId: Long, delta: Int) = memberDao.addPoints(memberId, delta)
    suspend fun deductMemberPoints(memberId: Long, delta: Int) = memberDao.deductPoints(memberId, delta)
    suspend fun getMemberCount(): Int = memberDao.getMemberCount()
    suspend fun getTotalMemberPoints(): Int = memberDao.getTotalPoints()
    suspend fun getPointsEarnRate(): Double = getSetting("points_earn_rate")?.toDoubleOrNull() ?: 200.0
    suspend fun getPointsRedeemRate(): Double = getSetting("points_redeem_rate")?.toDoubleOrNull() ?: 1.0

    // --- Vouchers & Promo ---
    fun getAllVouchers(): Flow<List<VoucherEntity>> = voucherDao.getAllVouchers()
    suspend fun getVoucherByCode(code: String): VoucherEntity? = voucherDao.getVoucherByCode(code.trim().uppercase())
    suspend fun insertVoucher(voucher: VoucherEntity) = voucherDao.insertVoucher(voucher)
    suspend fun updateVoucher(voucher: VoucherEntity) = voucherDao.updateVoucher(voucher)
    suspend fun deleteVoucher(code: String) = voucherDao.deleteVoucher(code)
    suspend fun incrementVoucherUsed(code: String) = voucherDao.incrementUsedCount(code)

    // --- Users & Auth ---
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    suspend fun getUserByPin(pin: String): UserEntity? = userDao.getUserByPin(pin)
    suspend fun getUserByUsername(username: String): UserEntity? = userDao.getUserByUsername(username.trim())
    suspend fun getUserByUsernameAndPin(username: String, pin: String): UserEntity? =
        userDao.getUserByUsernameAndPin(username.trim(), pin.trim())
    suspend fun insertUser(user: UserEntity): Long = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)

    // --- Categories & Menu ---
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    fun getAllMenus(): Flow<List<MenuEntity>> = menuDao.getAllMenus()
    fun getMenusByCategory(catId: Long): Flow<List<MenuEntity>> = menuDao.getMenusByCategory(catId)
    suspend fun insertMenu(menu: MenuEntity): Long = menuDao.insertMenu(menu)
    suspend fun updateMenu(menu: MenuEntity) = menuDao.updateMenu(menu)
    suspend fun deleteMenu(menu: MenuEntity) = menuDao.deleteMenu(menu)

    // --- Ingredients (Bahan Baku) ---
    fun getAllIngredients(): Flow<List<IngredientEntity>> = ingredientDao.getAllIngredients()
    fun getLowStockIngredients(): Flow<List<IngredientEntity>> = ingredientDao.getLowStockIngredients()
    suspend fun insertIngredient(ingredient: IngredientEntity): Long = ingredientDao.insertIngredient(ingredient)
    suspend fun updateIngredient(ingredient: IngredientEntity) = ingredientDao.updateIngredient(ingredient)
    suspend fun deleteIngredient(ingredient: IngredientEntity) = ingredientDao.deleteIngredient(ingredient)
    suspend fun addStock(ingredientId: Long, amount: Double) = ingredientDao.addStock(ingredientId, amount)

    // --- Recipes ---
    fun getAllRecipes(): Flow<List<RecipeEntity>> = recipeDao.getAllRecipes()
    suspend fun getRecipesForMenu(menuId: Long): List<RecipeEntity> = recipeDao.getRecipesForMenu(menuId)
    fun getRecipesForMenuFlow(menuId: Long): Flow<List<RecipeEntity>> = recipeDao.getRecipesForMenuFlow(menuId)
    suspend fun saveRecipesForMenu(menuId: Long, recipes: List<RecipeEntity>) = withContext(Dispatchers.IO) {
        database.withTransaction {
            recipeDao.deleteRecipesByMenuId(menuId)
            recipeDao.insertRecipes(recipes)
        }
    }

    // --- Settings ---
    fun getAllSettings(): Flow<List<SettingEntity>> = settingDao.getAllSettings()
    suspend fun getSetting(key: String): String? = settingDao.getSetting(key)?.value
    fun getSettingFlow(key: String): Flow<SettingEntity?> = settingDao.getSettingFlow(key)
    suspend fun setSetting(key: String, value: String) = settingDao.setSetting(SettingEntity(key, value))

    // --- Stock Validation ---
    suspend fun validateCartStock(cartItems: List<CartItem>): StockValidationResult = withContext(Dispatchers.IO) {
        val ingredientNeedMap = mutableMapOf<Long, Double>()

        for (cartItem in cartItems) {
            val recipes = recipeDao.getRecipesForMenu(cartItem.menu.id)
            for (recipe in recipes) {
                val needed = recipe.amountRequired * cartItem.quantity
                ingredientNeedMap[recipe.ingredientId] = (ingredientNeedMap[recipe.ingredientId] ?: 0.0) + needed
            }
        }

        val insufficient = mutableListOf<String>()
        for ((ingredientId, totalNeeded) in ingredientNeedMap) {
            val ingredient = ingredientDao.getIngredientById(ingredientId)
            if (ingredient != null && ingredient.currentStock < totalNeeded) {
                insufficient.add("${ingredient.name}: Butuh ${totalNeeded} ${ingredient.unit}, sisa ${ingredient.currentStock} ${ingredient.unit}")
            }
        }

        StockValidationResult(
            isSufficient = insufficient.isEmpty(),
            insufficientIngredients = insufficient
        )
    }

    // --- Atomic POS Transaction Processing ---
    suspend fun processTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                // 1. Insert Transaction header
                transactionDao.insertTransaction(transaction)

                // 2. Insert items
                transactionDao.insertTransactionItems(items)

                // 3. Real-time automatic stock deduction from recipes
                for (item in items) {
                    val recipes = recipeDao.getRecipesForMenu(item.menuId)
                    for (recipe in recipes) {
                        val amountToDeduct = recipe.amountRequired * item.quantity
                        ingredientDao.deductStock(recipe.ingredientId, amountToDeduct)
                    }
                }
            }

            // Check if auto-sync to Google Sheets is enabled
            val autoSync = settingDao.getSetting("auto_sync_sheets")?.value.toBoolean()
            if (autoSync) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        sheetsSyncService.syncPendingTransactions()
                    } catch (_: Exception) {}
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Transactions & Reporting ---
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    suspend fun getTransactionItems(transactionId: String): List<TransactionItemEntity> =
        transactionDao.getTransactionItems(transactionId)
    suspend fun getAllTransactionItems(): List<TransactionItemEntity> =
        transactionDao.getAllTransactionItems()
    suspend fun getTransactionsBetween(startDate: Long, endDate: Long): List<TransactionEntity> =
        transactionDao.getTransactionsBetween(startDate, endDate)

    suspend fun getSalesReports(startDate: Long, endDate: Long): ReportSummary = withContext(Dispatchers.IO) {
        val transactions = transactionDao.getTransactionsBetween(startDate, endDate)
        val allItems = transactionDao.getAllTransactionItems()

        val trxIdSet = transactions.map { it.id }.toSet()
        val filteredItems = allItems.filter { it.transactionId in trxIdSet }

        val totalRevenue = transactions.sumOf { it.totalAmount }
        val totalCost = filteredItems.sumOf { it.costPrice * it.quantity }
        val netProfit = totalRevenue - totalCost

        // Top selling items
        val menuCountMap = mutableMapOf<String, Int>()
        filteredItems.forEach {
            menuCountMap[it.menuName] = (menuCountMap[it.menuName] ?: 0) + it.quantity
        }
        val topSelling = menuCountMap.toList().sortedByDescending { it.second }.take(5)

        // Ingredient usage
        val allRecipes = recipeDao.getAllRecipes().first()
        val allIngredients = ingredientDao.getAllIngredients().first().associateBy { it.id }

        val ingredientUsage = mutableMapOf<Long, Double>()
        for (item in filteredItems) {
            val recipes = allRecipes.filter { it.menuId == item.menuId }
            for (rec in recipes) {
                ingredientUsage[rec.ingredientId] = (ingredientUsage[rec.ingredientId] ?: 0.0) + (rec.amountRequired * item.quantity)
            }
        }

        val usageSummary = ingredientUsage.mapNotNull { (ingId, qty) ->
            val ing = allIngredients[ingId] ?: return@mapNotNull null
            ing.name to "${String.format(Locale.US, "%.1f", qty)} ${ing.unit}"
        }

        ReportSummary(
            totalRevenue = totalRevenue,
            totalNetProfit = netProfit,
            transactionCount = transactions.size,
            topSellingItems = topSelling,
            ingredientUsageSummary = usageSummary
        )
    }

    suspend fun syncToGoogleSheets(): SyncResult {
        return sheetsSyncService.syncPendingTransactions()
    }

    suspend fun resetDatabaseToFactory(includeSampleData: Boolean = true, preserveAdminUser: UserEntity? = null) {
        com.example.data.local.InitialDataSeeder.resetDatabaseToFactory(database, includeSampleData, preserveAdminUser)
    }
}
