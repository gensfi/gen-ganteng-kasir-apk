package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.MenuEntity
import com.example.data.repository.CartItem
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: CafePosViewModel,
    onOpenCart: () -> Unit
) {
    val menus by viewModel.filteredMenus.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartSubtotal by viewModel.cartSubtotal.collectAsState()
    val lowStockItems by viewModel.lowStockIngredients.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showNotesDialogForMenu by remember { mutableStateOf<MenuEntity?>(null) }
    var itemNoteText by remember { mutableStateOf("") }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocalCafe,
                                contentDescription = null,
                                tint = CoffeeAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "KafePOS",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CoffeeBrownDark
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            // Cashier pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentUser?.role == "ADMIN") CoffeeBrownPrimary else CoffeeAmber.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "${currentUser?.name ?: "Kasir"} (${currentUser?.role ?: "KASIR"})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (currentUser?.role == "ADMIN") Color.White else CoffeeBrownDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.switchUser() },
                        modifier = Modifier.testTag("switch_user_button")
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = "Ganti Kasir / Login PIN")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Persistent Cart Bar at bottom when items exist
            if (cartItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val totalQty = cartItems.sumOf { it.quantity }
                            Text(
                                "$totalQty Item Pesanan",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralMedium
                            )
                            Text(
                                currency.format(cartSubtotal),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CoffeeBrownPrimary
                                )
                            )
                        }

                        Button(
                            onClick = onOpenCart,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                            modifier = Modifier
                                .testTag("view_cart_button")
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Buka Keranjang", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Low Stock Warning Banner if applicable
            if (lowStockItems.isNotEmpty()) {
                Surface(
                    color = CoffeeOrangeLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Peringatan Stok",
                            tint = CoffeeOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${lowStockItems.size} bahan baku menipis/habis: ${lowStockItems.take(2).joinToString { it.name }}",
                            fontSize = 12.sp,
                            color = CoffeeBrownDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Cari menu kopi, minuman, pastry...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CoffeeAmber,
                    unfocusedBorderColor = NeutralBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("pos_search_input")
            )

            // Category Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("Semua Menu") },
                        leadingIcon = {
                            Icon(Icons.Filled.RestaurantMenu, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoffeeBrownPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("category_chip_all")
                    )
                }

                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.id,
                        onClick = { viewModel.selectCategory(cat.id) },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            val icon = when (cat.iconName) {
                                "Coffee" -> Icons.Filled.LocalCafe
                                "Tea" -> Icons.Filled.EmojiFoodBeverage
                                "Bakery" -> Icons.Filled.BakeryDining
                                "Food" -> Icons.Filled.Fastfood
                                else -> Icons.Filled.Category
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoffeeBrownPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("category_chip_${cat.id}")
                    )
                }
            }

            // Menu Grid
            if (menus.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = NeutralMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Menu tidak ditemukan",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeutralMedium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pos_menu_grid")
                ) {
                    items(menus) { menu ->
                        MenuGridCard(
                            menu = menu,
                            currency = currency,
                            onAddToCart = { viewModel.addToCart(menu) },
                            onAddWithNotes = {
                                showNotesDialogForMenu = menu
                                itemNoteText = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Add with notes dialog
    showNotesDialogForMenu?.let { menu ->
        AlertDialog(
            onDismissRequest = { showNotesDialogForMenu = null },
            title = { Text(menu.name) },
            text = {
                Column {
                    Text("Tambah catatan khusus (misal: Less ice, less sugar, pisah cup):", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = itemNoteText,
                        onValueChange = { itemNoteText = it },
                        placeholder = { Text("Contoh: Gula 50%, Es sedikit") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addToCart(menu, itemNoteText.trim())
                        showNotesDialogForMenu = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                ) {
                    Text("Tambah ke Keranjang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialogForMenu = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun MenuGridCard(
    menu: MenuEntity,
    currency: NumberFormat,
    onAddToCart: () -> Unit,
    onAddWithNotes: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("menu_card_${menu.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header icon badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocalCafe,
                    contentDescription = null,
                    tint = CoffeeBrownPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = menu.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeutralDark
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (menu.description.isNotBlank()) {
                Text(
                    text = menu.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(32.dp)
                )
            } else {
                Spacer(Modifier.height(32.dp))
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currency.format(menu.price),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CoffeeBrownPrimary
                    )
                )

                Row {
                    IconButton(
                        onClick = onAddWithNotes,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.EditNote,
                            contentDescription = "Catatan Khusus",
                            tint = CoffeeAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = CoffeeBrownPrimary,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onAddToCart() }
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Tambah",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
