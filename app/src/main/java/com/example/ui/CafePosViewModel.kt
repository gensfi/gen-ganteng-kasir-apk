package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.AppDatabase
import com.example.data.local.InitialDataSeeder
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.IngredientEntity
import com.example.data.local.entity.MenuEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UserEntity
import com.example.data.printer.BluetoothPrinterDevice
import com.example.data.printer.BluetoothPrinterHelper
import com.example.data.printer.PrinterStatus
import com.example.data.remote.GoogleSheetsSyncService
import com.example.data.remote.SyncResult
import com.example.data.repository.CafePosRepository
import com.example.data.repository.CartItem
import com.example.data.repository.ReportSummary
import com.example.data.share.WhatsAppShareHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CafeScreen {
    POS,
    INVENTORY,
    MENU_MANAGEMENT,
    REPORTS,
    PRINTER_SETTINGS,
    GOOGLE_SHEETS,
    SETTINGS
}

data class CompletedTransaction(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>,
    val receiptText: String
)

class CafePosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val sheetsSyncService = GoogleSheetsSyncService(db.transactionDao(), db.ingredientDao(), db.settingDao())
    val repository = CafePosRepository(db, sheetsSyncService)
    val printerHelper = BluetoothPrinterHelper(application)
    val backupRestoreManager = BackupRestoreManager(db)

    // Current Screen
    private val _currentScreen = MutableStateFlow(CafeScreen.POS)
    val currentScreen: StateFlow<CafeScreen> = _currentScreen.asStateFlow()

    // Current Logged-in User
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Screen change
    fun navigateTo(screen: CafeScreen) {
        if (_currentUser.value?.role == "KASIR" && screen != CafeScreen.POS && screen != CafeScreen.PRINTER_SETTINGS) {
            _currentScreen.value = CafeScreen.POS
            return
        }
        _currentScreen.value = screen
    }

    // Auth
    fun loginWithPin(pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByPin(pin)
            if (user != null) {
                _currentUser.value = user
                _currentScreen.value = CafeScreen.POS
                onResult(true, "Selamat datang, ${user.name} (${user.role})")
            } else {
                onResult(false, "PIN salah! Silakan coba lagi.")
            }
        }
    }

    fun switchUser() {
        _currentUser.value = null
        _currentScreen.value = CafeScreen.POS
    }

    // Categories & Menus
    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val menus: StateFlow<List<MenuEntity>> = repository.getAllMenus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ingredients: StateFlow<List<IngredientEntity>> = repository.getAllIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockIngredients: StateFlow<List<IngredientEntity>> = repository.getLowStockIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Category Filter in POS
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    fun selectCategory(id: Long?) {
        _selectedCategoryId.value = id
    }

    // Search query in POS
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Filtered Menus in POS
    val filteredMenus: StateFlow<List<MenuEntity>> = combine(menus, _selectedCategoryId, _searchQuery) { menuList, catId, query ->
        menuList.filter { menu ->
            val matchCategory = (catId == null || menu.categoryId == catId)
            val matchQuery = (query.isBlank() || menu.name.contains(query, ignoreCase = true) || menu.description.contains(query, ignoreCase = true))
            matchCategory && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _orderNotes = MutableStateFlow("")
    val orderNotes: StateFlow<String> = _orderNotes.asStateFlow()

    fun setCustomerName(name: String) { _customerName.value = name }
    fun setCustomerPhone(phone: String) { _customerPhone.value = phone }
    fun setOrderNotes(notes: String) { _orderNotes.value = notes }

    fun addToCart(menu: MenuEntity, notes: String = "") {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.menu.id == menu.id && it.notes == notes }
        if (index != -1) {
            val existing = currentList[index]
            currentList[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(CartItem(menu = menu, quantity = 1, notes = notes))
        }
        _cartItems.value = currentList
    }

    fun updateCartItemQuantity(cartItem: CartItem, newQty: Int) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.menu.id == cartItem.menu.id && it.notes == cartItem.notes }
        if (index != -1) {
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else {
                currentList[index] = cartItem.copy(quantity = newQty)
            }
            _cartItems.value = currentList
        }
    }

    fun removeFromCart(cartItem: CartItem) {
        updateCartItemQuantity(cartItem, 0)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _customerName.value = ""
        _customerPhone.value = ""
        _orderNotes.value = ""
    }

    // Calculations
    val cartSubtotal: StateFlow<Double> = _cartItems
        .map { list -> list.sumOf { it.menu.price * it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _taxRate = MutableStateFlow(10.0) // 10%
    val taxRate: StateFlow<Double> = _taxRate.asStateFlow()

    private val _serviceRate = MutableStateFlow(5.0) // 5%
    val serviceRate: StateFlow<Double> = _serviceRate.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    fun setDiscount(amount: Double) { _discountAmount.value = amount }

    // Last completed transaction for receipt dialog
    private val _lastTransaction = MutableStateFlow<CompletedTransaction?>(null)
    val lastTransaction: StateFlow<CompletedTransaction?> = _lastTransaction.asStateFlow()

    fun dismissReceipt() {
        _lastTransaction.value = null
    }

    // Stock validation check before payment
    fun checkStockSufficiency(onResult: (Boolean, List<String>) -> Unit) {
        viewModelScope.launch {
            val check = repository.validateCartStock(_cartItems.value)
            onResult(check.isSufficient, check.insufficientIngredients)
        }
    }

    // Process Payment
    fun processPayment(
        paymentMethod: String,
        cashGiven: Double,
        onSuccess: (CompletedTransaction) -> Unit,
        onError: (String) -> Unit
    ) {
        val itemsInCart = _cartItems.value
        if (itemsInCart.isEmpty()) {
            onError("Keranjang belanja masih kosong!")
            return
        }

        viewModelScope.launch {
            // Check stock first
            val stockCheck = repository.validateCartStock(itemsInCart)
            if (!stockCheck.isSufficient) {
                onError("Stok tidak mencukupi:\n" + stockCheck.insufficientIngredients.joinToString("\n"))
                return@launch
            }

            val subtotal = itemsInCart.sumOf { it.menu.price * it.quantity }
            val taxAmount = (subtotal * _taxRate.value) / 100.0
            val serviceAmount = (subtotal * _serviceRate.value) / 100.0
            val total = subtotal + taxAmount + serviceAmount - _discountAmount.value
            val change = if (paymentMethod == "CASH") maxOf(0.0, cashGiven - total) else 0.0

            val dateStr = SimpleDateFormat("yyyyMMdd-HHmmss", Locale("id", "ID")).format(Date())
            val trxId = "TRX-$dateStr"

            val user = _currentUser.value
            val cashierId = user?.id ?: 1L
            val cashierName = user?.name ?: "Kasir Default"

            val transactionEntity = TransactionEntity(
                id = trxId,
                cashierId = cashierId,
                cashierName = cashierName,
                timestamp = System.currentTimeMillis(),
                subtotal = subtotal,
                taxAmount = taxAmount,
                serviceChargeAmount = serviceAmount,
                discountAmount = _discountAmount.value,
                totalAmount = total,
                paymentMethod = paymentMethod,
                cashGiven = if (paymentMethod == "CASH") cashGiven else total,
                changeAmount = change,
                customerName = _customerName.value.ifBlank { null },
                customerPhone = _customerPhone.value.ifBlank { null },
                notes = _orderNotes.value.ifBlank { null }
            )

            val itemEntities = itemsInCart.map { cItem ->
                TransactionItemEntity(
                    transactionId = trxId,
                    menuId = cItem.menu.id,
                    menuName = cItem.menu.name,
                    price = cItem.menu.price,
                    costPrice = cItem.menu.costPrice,
                    quantity = cItem.quantity,
                    itemNotes = cItem.notes.ifBlank { null }
                )
            }

            val result = repository.processTransaction(transactionEntity, itemEntities)
            if (result.isSuccess) {
                val cafeName = repository.getSetting("cafe_name") ?: "Kopi Kenangan Senja"
                val cafeAddress = repository.getSetting("cafe_address") ?: ""
                val cafePhone = repository.getSetting("cafe_phone") ?: ""
                val footer = repository.getSetting("receipt_footer") ?: ""

                val receiptText = WhatsAppShareHelper.buildReceiptText(
                    transaction = transactionEntity,
                    items = itemEntities,
                    cafeName = cafeName,
                    cafeAddress = cafeAddress,
                    cafePhone = cafePhone,
                    footerMessage = footer
                )

                val completed = CompletedTransaction(
                    transaction = transactionEntity,
                    items = itemEntities,
                    receiptText = receiptText
                )

                _lastTransaction.value = completed
                clearCart()
                onSuccess(completed)
            } else {
                onError("Gagal memproses transaksi: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // Printer Management
    private val _printerStatus = MutableStateFlow<PrinterStatus>(PrinterStatus.Idle)
    val printerStatus: StateFlow<PrinterStatus> = _printerStatus.asStateFlow()

    private val _pairedPrinters = MutableStateFlow<List<BluetoothPrinterDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<BluetoothPrinterDevice>> = _pairedPrinters.asStateFlow()

    private val _paperSize = MutableStateFlow(58)
    val paperSize: StateFlow<Int> = _paperSize.asStateFlow()

    fun setPaperSize(size: Int) {
        _paperSize.value = size
        viewModelScope.launch {
            repository.setSetting("printer_paper_size", size.toString())
        }
    }

    fun scanPairedPrinters() {
        val list = printerHelper.getPairedDevices()
        _pairedPrinters.value = list
    }

    fun connectPrinter(address: String) {
        _printerStatus.value = PrinterStatus.Connecting
        viewModelScope.launch {
            val result = printerHelper.connect(address)
            result.fold(
                onSuccess = { name ->
                    _printerStatus.value = PrinterStatus.Connected(name)
                },
                onFailure = { err ->
                    _printerStatus.value = PrinterStatus.Error(err.localizedMessage ?: "Gagal terhubung")
                }
            )
        }
    }

    fun disconnectPrinter() {
        printerHelper.disconnect()
        _printerStatus.value = PrinterStatus.Idle
    }

    fun printCurrentReceipt(completed: CompletedTransaction, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cafeName = repository.getSetting("cafe_name") ?: "Kopi Kenangan Senja"
            val cafeAddress = repository.getSetting("cafe_address") ?: ""
            val cafePhone = repository.getSetting("cafe_phone") ?: ""
            val footer = repository.getSetting("receipt_footer") ?: ""

            val res = printerHelper.printTransactionReceipt(
                transaction = completed.transaction,
                items = completed.items,
                cafeName = cafeName,
                cafeAddress = cafeAddress,
                cafePhone = cafePhone,
                footerMessage = footer,
                paperSize = _paperSize.value
            )

            res.fold(
                onSuccess = { onResult(true, "Struk berhasil dicetak!") },
                onFailure = { onResult(false, it.localizedMessage ?: "Gagal mencetak") }
            )
        }
    }

    fun testPrint(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cafeName = repository.getSetting("cafe_name") ?: "Kopi Kenangan Senja"
            val res = printerHelper.printTestReceipt(cafeName, _paperSize.value)
            res.fold(
                onSuccess = { onResult(true, "Test print berhasil!") },
                onFailure = { onResult(false, it.localizedMessage ?: "Gagal test print") }
            )
        }
    }

    // Google Sheets Sync
    private val _syncState = MutableStateFlow<String>("Belum disinkronisasi")
    val syncState: StateFlow<String> = _syncState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun triggerGoogleSheetsSync(onResult: (Boolean, String) -> Unit) {
        _isSyncing.value = true
        _syncState.value = "Sedang sinkronisasi..."
        viewModelScope.launch {
            val res = repository.syncToGoogleSheets()
            _isSyncing.value = false
            when (res) {
                is SyncResult.Success -> {
                    _syncState.value = "Berhasil: ${res.message}"
                    onResult(true, res.message)
                }
                is SyncResult.Error -> {
                    _syncState.value = "Gagal: ${res.message}"
                    onResult(false, res.message)
                }
            }
        }
    }

    // Reports calculation
    private val _reportSummary = MutableStateFlow<ReportSummary?>(null)
    val reportSummary: StateFlow<ReportSummary?> = _reportSummary.asStateFlow()

    fun loadReports(periodDays: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            if (periodDays == 0) {
                // Today from 00:00
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            } else {
                cal.add(Calendar.DAY_OF_YEAR, -periodDays)
            }
            val start = cal.timeInMillis
            val summary = repository.getSalesReports(start, end)
            _reportSummary.value = summary
        }
    }

    // Inventory operations
    fun addIngredientStock(ingredientId: Long, amount: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addStock(ingredientId, amount)
            onDone()
        }
    }

    fun saveIngredient(ingredient: IngredientEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            if (ingredient.id == 0L) {
                repository.insertIngredient(ingredient)
            } else {
                repository.updateIngredient(ingredient)
            }
            onDone()
        }
    }

    fun deleteIngredient(ingredient: IngredientEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteIngredient(ingredient)
            onDone()
        }
    }

    // Menu & Category operations
    fun saveMenu(menu: MenuEntity, recipes: List<RecipeEntity>, onDone: () -> Unit) {
        viewModelScope.launch {
            val menuId = if (menu.id == 0L) {
                repository.insertMenu(menu)
            } else {
                repository.updateMenu(menu)
                menu.id
            }

            val adjustedRecipes = recipes.map { it.copy(menuId = menuId) }
            repository.saveRecipesForMenu(menuId, adjustedRecipes)
            onDone()
        }
    }

    fun deleteMenu(menu: MenuEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteMenu(menu)
            onDone()
        }
    }

    fun saveCategory(category: CategoryEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            if (category.id == 0L) {
                repository.insertCategory(category)
            } else {
                repository.updateCategory(category)
            }
            onDone()
        }
    }

    fun deleteCategory(category: CategoryEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            onDone()
        }
    }

    // Initialization
    init {
        viewModelScope.launch {
            InitialDataSeeder.seedIfNeeded(db)
            // Auto login default admin for immediate preview usability
            val admin = db.userDao().getUserByUsername("admin")
            _currentUser.value = admin

            // Load settings
            val savedTax = repository.getSetting("tax_percentage")?.toDoubleOrNull() ?: 10.0
            val savedService = repository.getSetting("service_charge_percentage")?.toDoubleOrNull() ?: 5.0
            val savedPaper = repository.getSetting("printer_paper_size")?.toIntOrNull() ?: 58
            _taxRate.value = savedTax
            _serviceRate.value = savedService
            _paperSize.value = savedPaper

            // Load initial report
            loadReports(0)
        }
    }
}
