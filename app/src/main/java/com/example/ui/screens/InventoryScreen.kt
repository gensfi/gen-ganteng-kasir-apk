package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.IngredientEntity
import com.example.data.local.entity.IngredientPurchaseEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val ingredients by viewModel.ingredients.collectAsState()
    val lowStockItems by viewModel.lowStockIngredients.collectAsState()
    val purchases by viewModel.ingredientPurchases.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Stok Bahan, 1: Riwayat Pembelian
    var searchQuery by remember { mutableStateOf("") }
    var purchaseSearchQuery by remember { mutableStateOf("") }

    var showPurchaseDialogFor by remember { mutableStateOf<IngredientEntity?>(null) }
    var showAddEditDialog by remember { mutableStateOf<IngredientEntity?>(null) }
    var isNewIngredient by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }
    val dateTimeFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    // Sorted stock list
    val sortedList = remember(ingredients, searchQuery) {
        val base = if (searchQuery.isBlank()) ingredients
        else ingredients.filter { it.name.contains(searchQuery, ignoreCase = true) }

        base.sortedWith(
            compareBy<IngredientEntity> { ing ->
                when {
                    ing.currentStock <= 0.0 -> 0 // HABIS TOTAL
                    ing.currentStock <= ing.minStock -> 1 // TIDAK AMAN
                    ing.currentStock <= ing.minStock * 1.5 -> 2 // WASPADA
                    else -> 3 // AMAN
                }
            }.thenBy { ing ->
                if (ing.minStock <= 0.0) ing.currentStock else (ing.currentStock / ing.minStock)
            }.thenBy { ing ->
                ing.name
            }
        )
    }

    // Filtered purchases
    val filteredPurchases = remember(purchases, purchaseSearchQuery) {
        if (purchaseSearchQuery.isBlank()) purchases
        else purchases.filter {
            it.ingredientName.contains(purchaseSearchQuery, ignoreCase = true) ||
                    it.supplierName.contains(purchaseSearchQuery, ignoreCase = true) ||
                    it.cashierName.contains(purchaseSearchQuery, ignoreCase = true) ||
                    it.notes.contains(purchaseSearchQuery, ignoreCase = true)
        }
    }

    val totalPurchaseCost = remember(purchases) { purchases.sumOf { it.totalCost } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventori Bahan Baku", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (selectedTab == 1 || purchases.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (purchases.isEmpty()) {
                                    Toast.makeText(context, "Belum ada riwayat pembelian untuk diekspor", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                isGeneratingPdf = true
                                coroutineScope.launch {
                                    try {
                                        val cafeName = viewModel.repository.getSetting("cafe_name") ?: "Rencana Kopi"
                                        val cafeAddress = viewModel.repository.getSetting("cafe_address") ?: "Sistem Kasir & Logistik"
                                        val cafePhone = viewModel.repository.getSetting("cafe_phone") ?: "0812-3456-7890"

                                        val pdfData = PdfReportGenerator.PdfPurchaseReportData(
                                            cafeName = cafeName,
                                            cafeAddress = cafeAddress,
                                            cafePhone = cafePhone,
                                            printedDateFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date()),
                                            purchases = filteredPurchases.ifEmpty { purchases },
                                            totalCost = (filteredPurchases.ifEmpty { purchases }).sumOf { it.totalCost }
                                        )

                                        val file = PdfReportGenerator.generatePurchasesPdf(context, pdfData)
                                        if (file != null) {
                                            PdfReportGenerator.openPdf(context, file)
                                        } else {
                                            Toast.makeText(context, "Gagal mencetak PDF pembelian", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGeneratingPdf = false
                                    }
                                }
                            },
                            enabled = !isGeneratingPdf
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Ekspor PDF Riwayat Pembelian", tint = BrightRed)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (currentUser?.role == "ADMIN" && selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        isNewIngredient = true
                        showAddEditDialog = IngredientEntity(
                            id = 0,
                            name = "",
                            unit = "Gram",
                            currentStock = 1000.0,
                            minStock = 200.0,
                            costPerUnit = 100.0
                        )
                    },
                    containerColor = CoffeeBrownPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_ingredient_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah Bahan Baku")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CoffeeBrownPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Stok Bahan (${ingredients.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Riwayat Pembelian (${purchases.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // TAB 0: STOK BAHAN BAKU
                // KPI Cards row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InventoryKpiCard(
                        title = "Total Bahan",
                        value = "${ingredients.size}",
                        color = CoffeeBrownPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    InventoryKpiCard(
                        title = "Stok Aman",
                        value = "${ingredients.count { it.currentStock > it.minStock }}",
                        color = CoffeeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    InventoryKpiCard(
                        title = "Stok Menipis",
                        value = "${lowStockItems.size}",
                        color = if (lowStockItems.isNotEmpty()) CoffeeRed else NeutralMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari bahan (Kopi, Susu, Sirup, Cup...)") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Status order info banner
                Surface(
                    color = PrimaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = null, tint = CoffeeBrownDark, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Status: 🚨 Tidak Aman / Habis ➔ ⚠️ Waspada ➔ ✅ Aman",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoffeeBrownDark
                        )
                    }
                }

                // Ingredient List
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedList, key = { it.id }) { ing ->
                        val isOut = ing.currentStock <= 0.0
                        val isUnsafe = ing.currentStock <= ing.minStock
                        val isWarning = !isUnsafe && ing.currentStock <= ing.minStock * 1.5

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isUnsafe -> CoffeeRedLight.copy(alpha = 0.35f)
                                    isWarning -> CoffeeOrangeLight.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ingredient_item_${ing.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            ing.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            "HPP / Modal: ${currency.format(ing.costPerUnit)} / ${ing.unit}",
                                            fontSize = 12.sp,
                                            color = NeutralMedium
                                        )
                                    }

                                    // Status Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isOut -> BrightRed
                                            isUnsafe -> CoffeeRedLight
                                            isWarning -> CoffeeOrangeLight
                                            else -> CoffeeGreenLight
                                        }
                                    ) {
                                        Text(
                                            text = when {
                                                isOut -> "🚨 HABIS"
                                                isUnsafe -> "🚨 TIDAK AMAN"
                                                isWarning -> "⚠️ WASPADA"
                                                else -> "✅ AMAN"
                                            },
                                            color = when {
                                                isOut -> Color.White
                                                isUnsafe -> CoffeeRed
                                                isWarning -> CoffeeOrange
                                                else -> CoffeeGreen
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Stock progress
                                val percent = if (ing.minStock > 0) {
                                    (ing.currentStock / (ing.minStock * 2)).coerceIn(0.0, 1.0).toFloat()
                                } else 1f

                                LinearProgressIndicator(
                                    progress = { percent },
                                    color = if (isUnsafe) CoffeeRed else if (isWarning) CoffeeOrange else CoffeeGreen,
                                    trackColor = NeutralBorder,
                                    modifier = Modifier.fillMaxWidth().height(6.dp)
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Stok: ${String.format(Locale.US, "%.1f", ing.currentStock)} ${ing.unit} (Min: ${ing.minStock} ${ing.unit})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isUnsafe) CoffeeRed else if (isWarning) CoffeeOrange else NeutralDark
                                    )

                                    Row {
                                        // Button Beli / Restock
                                        Button(
                                            onClick = { showPurchaseDialogFor = ing },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                                        ) {
                                            Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Beli / Restock", fontSize = 12.sp)
                                        }

                                        if (currentUser?.role == "ADMIN") {
                                            IconButton(onClick = {
                                                isNewIngredient = false
                                                showAddEditDialog = ing
                                            }) {
                                                Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = CoffeeAmber)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: RIWAYAT PEMBELIAN BAHAN BAKU
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(10.dp))

                    // Summary Box Belanja Bahan
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Pengeluaran Belanja Bahan", fontSize = 11.5.sp, color = CoffeeBrownDark)
                                Text(
                                    currency.format(totalPurchaseCost),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoffeeBrownPrimary
                                )
                                Text("${purchases.size} transaksi pembelian tercatat", fontSize = 10.5.sp, color = NeutralMedium)
                            }

                            Button(
                                onClick = {
                                    if (purchases.isEmpty()) {
                                        Toast.makeText(context, "Belum ada data pembelian untuk diekspor", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isGeneratingPdf = true
                                    coroutineScope.launch {
                                        try {
                                            val cafeName = viewModel.repository.getSetting("cafe_name") ?: "Rencana Kopi"
                                            val cafeAddress = viewModel.repository.getSetting("cafe_address") ?: "Sistem Kasir & Logistik"
                                            val cafePhone = viewModel.repository.getSetting("cafe_phone") ?: "0812-3456-7890"

                                            val pdfData = PdfReportGenerator.PdfPurchaseReportData(
                                                cafeName = cafeName,
                                                cafeAddress = cafeAddress,
                                                cafePhone = cafePhone,
                                                printedDateFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date()),
                                                purchases = filteredPurchases.ifEmpty { purchases },
                                                totalCost = (filteredPurchases.ifEmpty { purchases }).sumOf { it.totalCost }
                                            )

                                            val file = PdfReportGenerator.generatePurchasesPdf(context, pdfData)
                                            if (file != null) {
                                                PdfReportGenerator.openPdf(context, file)
                                            } else {
                                                Toast.makeText(context, "Gagal membuat PDF pembelian", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingPdf = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightRed),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ekspor PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Search purchase
                    OutlinedTextField(
                        value = purchaseSearchQuery,
                        onValueChange = { purchaseSearchQuery = it },
                        placeholder = { Text("Cari riwayat (bahan, toko/supplier, kasir...)") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (purchaseSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { purchaseSearchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Hapus")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    if (filteredPurchases.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeutralLight,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = NeutralMedium, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada riwayat pembelian bahan baku.", fontWeight = FontWeight.SemiBold, color = NeutralDark)
                                Text("Klik tombol 'Beli / Restock' pada tab Stok Bahan untuk mencatat pengadaan baru.", fontSize = 12.sp, color = NeutralMedium)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredPurchases, key = { it.id }) { purchase ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    purchase.ingredientName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    dateTimeFormat.format(Date(purchase.timestamp)),
                                                    fontSize = 11.sp,
                                                    color = NeutralMedium
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    currency.format(purchase.totalCost),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.5.sp,
                                                    color = BrightRed
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = CoffeeGreenLight
                                                ) {
                                                    Text(
                                                        "+${purchase.quantity} ${purchase.unit}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CoffeeGreen,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(6.dp))
                                        HorizontalDivider(color = NeutralBorder.copy(alpha = 0.5f))
                                        Spacer(Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Harga Satuan: ${currency.format(purchase.costPerUnit)} / ${purchase.unit}",
                                                fontSize = 11.5.sp,
                                                color = NeutralMedium
                                            )
                                            Text(
                                                "Dicatat oleh: 👤 ${purchase.cashierName.ifBlank { "Admin" }}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CoffeeBrownPrimary
                                            )
                                        }

                                        if (purchase.supplierName.isNotBlank() || purchase.notes.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                buildString {
                                                    if (purchase.supplierName.isNotBlank()) append("Toko/Supplier: ${purchase.supplierName}")
                                                    if (purchase.supplierName.isNotBlank() && purchase.notes.isNotBlank()) append(" • ")
                                                    if (purchase.notes.isNotBlank()) append("Ket: ${purchase.notes}")
                                                },
                                                fontSize = 11.sp,
                                                color = NeutralMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: PEMBELIAN / RESTOCK BAHAN BAKU
    showPurchaseDialogFor?.let { ing ->
        var quantityText by remember { mutableStateOf("500") }
        var costPerUnitText by remember { mutableStateOf(ing.costPerUnit.toInt().toString()) }
        var supplierName by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        val enteredQty = quantityText.toDoubleOrNull() ?: 0.0
        val enteredCost = costPerUnitText.toDoubleOrNull() ?: 0.0
        val calculatedTotal = enteredQty * enteredCost

        AlertDialog(
            onDismissRequest = { showPurchaseDialogFor = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null, tint = CoffeeBrownPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Pembelian Bahan: ${ing.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = PrimaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Stok saat ini: ${ing.currentStock} ${ing.unit} (Min: ${ing.minStock} ${ing.unit})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CoffeeBrownDark,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // Input Jumlah Pembelian
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Jumlah Dibeli / Masuk (${ing.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(ing.unit) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick buttons for qty
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(100, 250, 500, 1000).forEach { amt ->
                            OutlinedButton(
                                onClick = { quantityText = amt.toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("+$amt", fontSize = 11.sp)
                            }
                        }
                    }

                    // Input Harga Beli / Modal per Unit
                    OutlinedTextField(
                        value = costPerUnitText,
                        onValueChange = { costPerUnitText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Harga Modal Beli per ${ing.unit}") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Total Biaya Box
                    Surface(
                        color = CoffeeRedLight.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Biaya Pembelian:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                currency.format(calculatedTotal),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrightRed
                            )
                        }
                    }

                    // Supplier / Toko
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Toko / Supplier (Opsional)") },
                        placeholder = { Text("Misal: Toko Kopi Sejahtera") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Catatan
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan / Keterangan (Opsional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Kasir yang mencatat
                    Text(
                        "Petugas yang mencatat: 👤 ${currentUser?.name ?: currentUser?.username ?: "Admin"}",
                        fontSize = 11.sp,
                        color = NeutralMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredQty <= 0) {
                            Toast.makeText(context, "Jumlah pembelian harus lebih dari 0", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.purchaseIngredient(
                            ingredientId = ing.id,
                            quantity = enteredQty,
                            costPerUnit = enteredCost,
                            supplier = supplierName.trim(),
                            notes = notes.trim()
                        ) { success ->
                            if (success) {
                                Toast.makeText(context, "Pembelian berhasil disimpan! Stok bertambah +$enteredQty ${ing.unit}", Toast.LENGTH_SHORT).show()
                                showPurchaseDialogFor = null
                            } else {
                                Toast.makeText(context, "Gagal mencatat pembelian", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                ) {
                    Text("Simpan Pembelian")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialogFor = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Dialog: TAMBAH / EDIT BAHAN BAKU
    showAddEditDialog?.let { ing ->
        var name by remember { mutableStateOf(ing.name) }
        var unit by remember { mutableStateOf(ing.unit) }
        var currentStock by remember { mutableStateOf(ing.currentStock.toString()) }
        var minStock by remember { mutableStateOf(ing.minStock.toString()) }
        var costPerUnit by remember { mutableStateOf(ing.costPerUnit.toString()) }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = null },
            title = { Text(if (isNewIngredient) "Tambah Bahan Baku Baru" else "Edit Bahan Baku") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Bahan (misal: Biji Kopi Arabica)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Unit selector
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Gram", "ML", "Pcs", "Sachet").forEach { u ->
                            FilterChip(
                                selected = unit == u,
                                onClick = { unit = u },
                                label = { Text(u, fontSize = 12.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentStock,
                            onValueChange = { currentStock = it },
                            label = { Text("Stok Sekarang") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minStock,
                            onValueChange = { minStock = it },
                            label = { Text("Batas Minimum") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = costPerUnit,
                        onValueChange = { costPerUnit = it },
                        label = { Text("Harga Modal per $unit (HPP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = ing.copy(
                            name = name.trim(),
                            unit = unit,
                            currentStock = currentStock.toDoubleOrNull() ?: 0.0,
                            minStock = minStock.toDoubleOrNull() ?: 0.0,
                            costPerUnit = costPerUnit.toDoubleOrNull() ?: 0.0
                        )
                        viewModel.saveIngredient(updated) {
                            showAddEditDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun InventoryKpiCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = NeutralMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
