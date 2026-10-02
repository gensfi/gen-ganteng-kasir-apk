package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.IngredientEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val ingredients by viewModel.ingredients.collectAsState()
    val lowStockItems by viewModel.lowStockIngredients.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showRestockDialogFor by remember { mutableStateOf<IngredientEntity?>(null) }
    var showAddEditDialog by remember { mutableStateOf<IngredientEntity?>(null) }
    var isNewIngredient by remember { mutableStateOf(false) }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    val filteredList = remember(ingredients, searchQuery) {
        if (searchQuery.isBlank()) ingredients
        else ingredients.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventori Bahan Baku", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (currentUser?.role == "ADMIN") {
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
            // KPI Cards row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

            // Stock reduction notice
            Surface(
                color = PrimaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoMode, contentDescription = null, tint = CoffeeBrownDark, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Otomatis berkurang saat transaksi POS berhasil berdasarkan resep.",
                        fontSize = 11.sp,
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
                items(filteredList) { ing ->
                    val isLow = ing.currentStock <= ing.minStock
                    val isOut = ing.currentStock <= 0.0

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                        "HPP: ${currency.format(ing.costPerUnit)} / ${ing.unit}",
                                        fontSize = 12.sp,
                                        color = NeutralMedium
                                    )
                                }

                                // Status Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isOut -> CoffeeRedLight
                                        isLow -> CoffeeOrangeLight
                                        else -> CoffeeGreenLight
                                    }
                                ) {
                                    Text(
                                        text = when {
                                            isOut -> "HABIS"
                                            isLow -> "MENIPIS"
                                            else -> "AMAN"
                                        },
                                        color = when {
                                            isOut -> CoffeeRed
                                            isLow -> CoffeeOrange
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
                                color = if (isLow) CoffeeOrange else CoffeeGreen,
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
                                    color = if (isLow) CoffeeRed else NeutralDark
                                )

                                Row {
                                    FilledTonalButton(
                                        onClick = { showRestockDialogFor = ing },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Restock", fontSize = 12.sp)
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
        }
    }

    // Quick Restock Dialog
    showRestockDialogFor?.let { ing ->
        var restockAmountText by remember { mutableStateOf("100") }

        AlertDialog(
            onDismissRequest = { showRestockDialogFor = null },
            title = { Text("Restock: ${ing.name}") },
            text = {
                Column {
                    Text("Tambahkan jumlah stok masuk (${ing.unit}):", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restockAmountText,
                        onValueChange = { restockAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(ing.unit) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50, 100, 500, 1000).forEach { amt ->
                            OutlinedButton(
                                onClick = { restockAmountText = amt.toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("+$amt", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = restockAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addIngredientStock(ing.id, amount) {
                                showRestockDialogFor = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                ) {
                    Text("Simpan Restock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestockDialogFor = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Add / Edit Ingredient Dialog
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
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
