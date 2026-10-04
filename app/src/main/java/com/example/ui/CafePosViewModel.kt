package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupFileInfo
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
import com.example.data.local.entity.VoucherEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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
    SETTINGS,
    CASHIER_SUMMARY,
    VOUCHER_MANAGEMENT
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
        if (_currentUser.value?.role == "KASIR" && screen !in listOf(CafeScreen.POS, CafeScreen.PRINTER_SETTINGS, CafeScreen.CASHIER_SUMMARY)) {
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

    // Change Password / PIN for all roles
    fun changeUserPassword(
        oldPin: String,
        newPin: String,
        confirmPin: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onResult(false, "Sesi pengguna tidak valid. Silakan login kembali.")
                return@launch
            }

            if (user.pin != oldPin.trim()) {
                onResult(false, "PIN / Password lama tidak sesuai!")
                return@launch
            }

            val trimmedNew = newPin.trim()
            if (trimmedNew.length < 4) {
                onResult(false, "PIN baru minimal 4 angka / karakter!")
                return@launch
            }

            if (trimmedNew != confirmPin.trim()) {
                onResult(false, "Konfirmasi PIN baru tidak sama!")
                return@launch
            }

            if (trimmedNew == oldPin.trim()) {
                onResult(false, "PIN baru tidak boleh sama dengan PIN lama!")
                return@launch
            }

            val updatedUser = user.copy(pin = trimmedNew)
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser
            onResult(true, "PIN / Password untuk akun ${user.name} berhasil diperbarui!")
        }
    }

    // Admin can reset any user's PIN
    fun adminResetUserPassword(
        targetUser: UserEntity,
        newPin: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (_currentUser.value?.role != "ADMIN") {
                onResult(false, "Hanya Owner / Admin yang dapat mereset PIN staf lain!")
                return@launch
            }

            val trimmedNew = newPin.trim()
            if (trimmedNew.length < 4) {
                onResult(false, "PIN baru minimal 4 angka / karakter!")
                return@launch
            }

            val updatedUser = targetUser.copy(pin = trimmedNew)
            repository.updateUser(updatedUser)
            if (_currentUser.value?.id == targetUser.id) {
                _currentUser.value = updatedUser
            }
            onResult(true, "PIN untuk ${targetUser.name} berhasil diperbarui ke: $trimmedNew")
        }
    }

    // Universal Password Reset with Master Security Key
    fun resetUserPassword(
        targetUser: UserEntity,
        securityCode: String,
        newPin: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val allUsers = repository.getAllUsers().first()
            val adminUser = allUsers.find { it.role == "ADMIN" }
            val cleanCode = securityCode.trim()
            val isValidMaster = cleanCode == "8888" ||
                    cleanCode == "1234" ||
                    cleanCode.equals("RENCANAKOPI", ignoreCase = true) ||
                    (adminUser != null && cleanCode == adminUser.pin)

            if (!isValidMaster) {
                onResult(false, "Kode Keamanan Master salah! Gunakan master PIN: 8888")
                return@launch
            }

            val trimmedPin = newPin.trim()
            if (trimmedPin.length < 4) {
                onResult(false, "PIN baru minimal 4 angka / karakter!")
                return@launch
            }

            val updated = targetUser.copy(pin = trimmedPin)
            repository.updateUser(updated)
            if (_currentUser.value?.id == targetUser.id) {
                _currentUser.value = updated
            }
            onResult(true, "PIN untuk ${targetUser.name} berhasil di-reset ke: $trimmedPin")
        }
    }

    // Update Tax and Service settings immediately updating active rates and database
    fun updateTaxAndService(
        taxStr: String,
        serviceStr: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cleanTax = taxStr.replace(',', '.').trim()
                val cleanService = serviceStr.replace(',', '.').trim()

                val taxVal = cleanTax.toDoubleOrNull() ?: 0.0
                val serviceVal = cleanService.toDoubleOrNull() ?: 0.0

                if (taxVal < 0 || taxVal > 100) {
                    onResult(false, "Persentase pajak harus antara 0% dan 100%")
                    return@launch
                }
                if (serviceVal < 0 || serviceVal > 100) {
                    onResult(false, "Persentase layanan harus antara 0% dan 100%")
                    return@launch
                }

                repository.setSetting("tax_percentage", taxVal.toString())
                repository.setSetting("service_charge_percentage", serviceVal.toString())

                _taxRate.value = taxVal
                _serviceRate.value = serviceVal

                onResult(true, "Pajak ($taxVal%) dan Layanan ($serviceVal%) berhasil disimpan!")
            } catch (e: Exception) {
                onResult(false, "Gagal menyimpan: ${e.message}")
            }
        }
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
        recalculateDiscount()
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
            recalculateDiscount()
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
        clearAllDiscounts()
    }

    // Calculations
    val cartSubtotal: StateFlow<Double> = _cartItems
        .map { list -> list.sumOf { it.menu.price * it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _taxRate = MutableStateFlow(10.0) // 10%
    val taxRate: StateFlow<Double> = _taxRate.asStateFlow()

    private val _serviceRate = MutableStateFlow(5.0) // 5%
    val serviceRate: StateFlow<Double> = _serviceRate.asStateFlow()

    // Voucher & Manual Discount
    val vouchers: StateFlow<List<VoucherEntity>> = repository.getAllVouchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appliedVoucher = MutableStateFlow<VoucherEntity?>(null)
    val appliedVoucher: StateFlow<VoucherEntity?> = _appliedVoucher.asStateFlow()

    private val _manualDiscountType = MutableStateFlow<String>("PERCENT") // "PERCENT" or "NOMINAL"
    val manualDiscountType: StateFlow<String> = _manualDiscountType.asStateFlow()

    private val _manualDiscountValue = MutableStateFlow(0.0)
    val manualDiscountValue: StateFlow<Double> = _manualDiscountValue.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    fun setDiscount(amount: Double) { _discountAmount.value = maxOf(0.0, amount) }

    fun applyVoucher(code: String, onResult: (Boolean, String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(false, "Masukkan kode voucher!")
            return
        }
        viewModelScope.launch {
            val v = repository.getVoucherByCode(cleanCode)
            if (v == null) {
                onResult(false, "Voucher '$cleanCode' tidak ditemukan!")
                return@launch
            }
            if (!v.isActive) {
                onResult(false, "Voucher '$cleanCode' sedang tidak aktif!")
                return@launch
            }
            if (v.isExpired()) {
                onResult(false, "Voucher '$cleanCode' telah kedaluwarsa!")
                return@launch
            }
            if (v.isQuotaExceeded()) {
                onResult(false, "Kuota voucher '$cleanCode' sudah habis!")
                return@launch
            }
            val subtotal = cartSubtotal.value
            if (subtotal < v.minPurchase) {
                onResult(false, "Minimal belanja untuk voucher ini adalah Rp ${v.minPurchase.toLong()}")
                return@launch
            }

            _appliedVoucher.value = v
            recalculateDiscount()
            onResult(true, "Voucher '$cleanCode' berhasil dipasang!")
        }
    }

    fun removeVoucher() {
        _appliedVoucher.value = null
        recalculateDiscount()
    }

    fun setManualDiscount(type: String, value: Double) {
        _manualDiscountType.value = type
        _manualDiscountValue.value = maxOf(0.0, value)
        recalculateDiscount()
    }

    fun clearAllDiscounts() {
        _appliedVoucher.value = null
        _manualDiscountValue.value = 0.0
        _discountAmount.value = 0.0
    }

    fun recalculateDiscount() {
        val subtotal = cartSubtotal.value
        var disc = 0.0

        _appliedVoucher.value?.let { v ->
            if (v.isValid(subtotal)) {
                disc += if (v.discountType.equals("PERCENT", ignoreCase = true)) {
                    (subtotal * v.discountValue) / 100.0
                } else {
                    v.discountValue
                }
            } else {
                _appliedVoucher.value = null
            }
        }

        if (_manualDiscountValue.value > 0) {
            disc += if (_manualDiscountType.value.equals("PERCENT", ignoreCase = true)) {
                (subtotal * _manualDiscountValue.value) / 100.0
            } else {
                _manualDiscountValue.value
            }
        }

        _discountAmount.value = minOf(subtotal, disc)
    }

    // Admin Voucher CRUD
    fun createOrUpdateVoucher(voucher: VoucherEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (voucher.code.isBlank()) {
                    onResult(false, "Kode voucher tidak boleh kosong!")
                    return@launch
                }
                if (voucher.discountValue <= 0) {
                    onResult(false, "Nilai diskon harus lebih besar dari 0!")
                    return@launch
                }
                if (voucher.discountType.equals("PERCENT", ignoreCase = true) && voucher.discountValue > 100) {
                    onResult(false, "Diskon persen maksimal 100%!")
                    return@launch
                }
                repository.insertVoucher(voucher.copy(code = voucher.code.trim().uppercase()))
                onResult(true, "Voucher '${voucher.code.uppercase()}' berhasil disimpan!")
            } catch (e: Exception) {
                onResult(false, "Gagal menyimpan voucher: ${e.message}")
            }
        }
    }

    fun deleteVoucher(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteVoucher(code)
                onResult(true, "Voucher '$code' berhasil dihapus!")
            } catch (e: Exception) {
                onResult(false, "Gagal menghapus: ${e.message}")
            }
        }
    }

    fun toggleVoucherStatus(voucher: VoucherEntity) {
        viewModelScope.launch {
            repository.updateVoucher(voucher.copy(isActive = !voucher.isActive))
        }
    }

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
                _appliedVoucher.value?.let { v ->
                    repository.incrementVoucherUsed(v.code)
                }
                clearCart()
                // Automatic data backup for business & inventory security
                viewModelScope.launch {
                    try {
                        if (backupRestoreManager.isAutoBackupEnabled()) {
                            backupRestoreManager.performAutoBackup(getApplication())
                            _lastAutoBackupTime.value = System.currentTimeMillis()
                            _backupFiles.value = backupRestoreManager.listBackups(getApplication())
                        }
                    } catch (_: Exception) {}
                }
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

    fun printCustomReceipt(text: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = printerHelper.printCustomText(text)
            res.fold(
                onSuccess = { onResult(true, "Berhasil mencetak ke printer") },
                onFailure = { onResult(false, it.localizedMessage ?: "Gagal mencetak") }
            )
        }
    }

    fun printSingleTransaction(trx: TransactionEntity, items: List<TransactionItemEntity>, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val cafeName = repository.getSetting("cafe_name") ?: "Rencana Kopi"
            val cafeAddress = repository.getSetting("cafe_address") ?: ""
            val cafePhone = repository.getSetting("cafe_phone") ?: ""
            val footer = repository.getSetting("receipt_footer") ?: ""
            val res = printerHelper.printTransactionReceipt(
                transaction = trx,
                items = items,
                cafeName = cafeName,
                cafeAddress = cafeAddress,
                cafePhone = cafePhone,
                footerMessage = footer,
                paperSize = _paperSize.value
            )
            res.fold(
                onSuccess = { onResult(true, "Struk berhasil dicetak!") },
                onFailure = { onResult(false, it.localizedMessage ?: "Gagal mencetak struk") }
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

    // Automatic & Manual Backup Management
    private val _isAutoBackupEnabled = MutableStateFlow(true)
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _lastAutoBackupTime = MutableStateFlow(0L)
    val lastAutoBackupTime: StateFlow<Long> = _lastAutoBackupTime.asStateFlow()

    private val _backupFiles = MutableStateFlow<List<BackupFileInfo>>(emptyList())
    val backupFiles: StateFlow<List<BackupFileInfo>> = _backupFiles.asStateFlow()

    fun refreshBackups() {
        viewModelScope.launch {
            _backupFiles.value = backupRestoreManager.listBackups(getApplication())
            _lastAutoBackupTime.value = backupRestoreManager.getLastAutoBackupTime()
            _isAutoBackupEnabled.value = backupRestoreManager.isAutoBackupEnabled()
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            backupRestoreManager.setAutoBackupEnabled(enabled)
            _isAutoBackupEnabled.value = enabled
        }
    }

    fun triggerAutoBackupNow(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = backupRestoreManager.performAutoBackup(getApplication())
                refreshBackups()
                onResult(true, "Data bisnis & inventaris berhasil dicadangkan ke ${file.name}")
            } catch (e: Exception) {
                onResult(false, "Gagal mencadangkan data: ${e.localizedMessage}")
            }
        }
    }

    fun triggerManualBackup(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = backupRestoreManager.performManualBackup(getApplication())
                refreshBackups()
                onResult(true, "File cadangan baru dibuat: ${file.name}")
            } catch (e: Exception) {
                onResult(false, "Gagal membuat cadangan: ${e.localizedMessage}")
            }
        }
    }

    fun restoreBackupFile(file: File, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = backupRestoreManager.restoreFromFile(file)
            if (res.isSuccess) {
                refreshBackups()
                onResult(true, "Data berhasil dipulihkan dari ${file.name}")
            } else {
                onResult(false, "Gagal memulihkan: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun shareBackup(file: File) {
        backupRestoreManager.shareBackupFile(getApplication(), file)
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

            // Auto backup settings & initial snapshot if needed
            _isAutoBackupEnabled.value = backupRestoreManager.isAutoBackupEnabled()
            _lastAutoBackupTime.value = backupRestoreManager.getLastAutoBackupTime()
            _backupFiles.value = backupRestoreManager.listBackups(getApplication())

            val now = System.currentTimeMillis()
            if (_isAutoBackupEnabled.value && (now - _lastAutoBackupTime.value > 86400000L)) {
                try {
                    backupRestoreManager.performAutoBackup(getApplication())
                    _lastAutoBackupTime.value = now
                    _backupFiles.value = backupRestoreManager.listBackups(getApplication())
                } catch (_: Exception) {}
            }

            // Load initial report
            loadReports(0)
        }
    }
}
