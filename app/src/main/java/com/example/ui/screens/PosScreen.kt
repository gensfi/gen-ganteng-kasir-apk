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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.MenuEntity
import com.example.data.repository.CartItem
import com.example.ui.CafePosViewModel
import com.example.ui.CafeScreen
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
    var showChangePasswordDialog by remember { mutableStateOf(false) }

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
                                "Rencana Kopi",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CoffeeBrownDark
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            // Cashier pill - Click to change password/PIN
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentUser?.role == "ADMIN") BrightOrangePrimary else BrightAmber.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable { showChangePasswordDialog = true }
                                    .testTag("cashier_pill_profile")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${currentUser?.name ?: "Kasir"} (${currentUser?.role ?: "KASIR"})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (currentUser?.role == "ADMIN") Color.White else CoffeeBrownDark
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Key,
                                        contentDescription = "Ganti PIN",
                                        tint = if (currentUser?.role == "ADMIN") Color.White else CoffeeBrownDark,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(CafeScreen.CASHIER_SUMMARY) },
                        modifier = Modifier.testTag("btn_top_cashier_summary")
                    ) {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Rekap Kas & Order Kasir",
                            tint = FreshGreen
                        )
                    }
                    IconButton(
                        onClick = { showChangePasswordDialog = true },
                        modifier = Modifier.testTag("btn_top_change_password")
                    ) {
                        Icon(
                            Icons.Filled.Key,
                            contentDescription = "Ganti PIN / Password",
                            tint = BrightOrangePrimary
                        )
                    }
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
                        label = { Text("Semua Menu", fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.RestaurantMenu,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedCategory == null) Color.White else BrightOrangePrimary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = NeutralDark,
                            selectedContainerColor = BrightOrangePrimary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == null,
                            borderColor = NeutralBorder,
                            selectedBorderColor = BrightOrangePrimary
                        ),
                        modifier = Modifier.testTag("category_chip_all")
                    )
                }

                items(categories) { cat ->
                    val isSelected = selectedCategory == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(cat.id) },
                        label = { Text(cat.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        leadingIcon = {
                            val icon = when (cat.iconName) {
                                "Coffee" -> Icons.Filled.LocalCafe
                                "Tea" -> Icons.Filled.EmojiFoodBeverage
                                "Bakery" -> Icons.Filled.BakeryDining
                                "Food" -> Icons.Filled.Fastfood
                                else -> Icons.Filled.Category
                            }
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) Color.White else BrightOrangePrimary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = NeutralDark,
                            selectedContainerColor = BrightOrangePrimary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = NeutralBorder,
                            selectedBorderColor = BrightOrangePrimary
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

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            viewModel = viewModel,
            onDismiss = { showChangePasswordDialog = false }
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("menu_card_${menu.id}")
    ) {
        Column {
            // Product Photo Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(Color(0xFFFFF7ED))
            ) {
                if (!menu.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(menu.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = menu.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFFEDD5), Color(0xFFFEF3C7))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.LocalCafe,
                            contentDescription = null,
                            tint = BrightOrangePrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Quick Add overlay button at top-right - Bright & Vibrant
                IconButton(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(34.dp)
                        .background(BrightOrangePrimary, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Tambah Cepat",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = menu.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeutralDark,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (menu.description.isNotBlank()) {
                    Text(
                        text = menu.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currency.format(menu.price),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = BrightOrangePrimary,
                            fontSize = 14.sp
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrightPeach,
                        modifier = Modifier.clickable { onAddWithNotes() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.EditNote,
                                contentDescription = "Catatan Khusus",
                                tint = BrightOrangeDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "Note",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrightOrangeDark
                            )
                        }
                    }
                }
            }
        }
    }
}
