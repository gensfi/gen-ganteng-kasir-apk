package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.MenuEntity
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

    // Sliding Header Visibility State:
    // Slide up -> hides header
    // Slide down -> shows header
    var isHeaderVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -14f && isHeaderVisible) {
                    // Scrolling up -> hide header
                    isHeaderVisible = false
                } else if (delta > 14f && !isHeaderVisible) {
                    // Scrolling down -> show header
                    isHeaderVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    Scaffold(
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            val screenWidth = maxWidth
            val isCompact = screenWidth < 360.dp
            val isTablet = screenWidth >= 600.dp

            // Adaptive Grid Columns according to device width and resolution
            val gridColumns = when {
                screenWidth < 330.dp -> GridCells.Fixed(1)
                screenWidth < 600.dp -> GridCells.Fixed(2)
                screenWidth < 840.dp -> GridCells.Fixed(3)
                screenWidth < 1100.dp -> GridCells.Fixed(4)
                else -> GridCells.Fixed(5)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Collapsible Menu Header (Circled area: Title/Role, Search Bar, and Categories)
                AnimatedVisibility(
                    visible = isHeaderVisible,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -15f) {
                                        isHeaderVisible = false
                                    } else if (dragAmount > 15f) {
                                        isHeaderVisible = true
                                    }
                                }
                            }
                    ) {
                        // 1. Top Bar Header (Store Title, Cashier Profile Pill, Action Buttons)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    Icons.Filled.LocalCafe,
                                    contentDescription = null,
                                    tint = CoffeeAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Rencana Kopi",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CoffeeBrownDark,
                                        fontSize = if (isCompact) 17.sp else 19.sp
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                // Cashier pill - Click to change password/PIN
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (currentUser?.role == "ADMIN") BrightOrangePrimary else BrightAmber.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .clickable { showChangePasswordDialog = true }
                                        .testTag("cashier_pill_profile")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${currentUser?.name ?: "Kasir"} (${currentUser?.role ?: "KASIR"})",
                                            fontSize = if (isCompact) 10.sp else 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (currentUser?.role == "ADMIN") Color.White else CoffeeBrownDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.navigateTo(CafeScreen.CASHIER_SUMMARY) },
                                    modifier = Modifier.testTag("btn_top_cashier_summary").size(38.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.AccountBalanceWallet,
                                        contentDescription = "Rekap Kas & Order Kasir",
                                        tint = FreshGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.switchUser() },
                                    modifier = Modifier.testTag("switch_user_button").size(38.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.AccountCircle,
                                        contentDescription = "Ganti Kasir / Pengguna",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // 2. Low Stock Warning Banner if applicable
                        if (lowStockItems.isNotEmpty()) {
                            Surface(
                                color = CoffeeOrangeLight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 3.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = "Peringatan Stok",
                                        tint = CoffeeOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${lowStockItems.size} bahan menipis: ${lowStockItems.take(2).joinToString { it.name }}",
                                        fontSize = 11.5.sp,
                                        color = CoffeeBrownDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 3. Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Cari menu kopi, minuman, pastry...", fontSize = if (isCompact) 13.sp else 14.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
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
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("pos_search_input")
                        )

                        // 4. Category Chips Row
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { viewModel.selectCategory(null) },
                                    label = { Text("Semua Menu", fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Medium, fontSize = if (isCompact) 12.sp else 13.sp) },
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
                                    label = { Text(cat.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = if (isCompact) 12.sp else 13.sp) },
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

                        // 5. Slide-up indicator / drag handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHeaderVisible = false }
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(NeutralLight, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 2.5.dp)
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Slide ke atas untuk hide",
                                    tint = NeutralMedium,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Slide ke atas untuk hide",
                                    fontSize = 11.sp,
                                    color = NeutralMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        HorizontalDivider(color = NeutralBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }

                // Collapsed Mini Bar when header is hidden (Slide down to reveal)
                AnimatedVisibility(
                    visible = !isHeaderVisible,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHeaderVisible = true }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 10f) {
                                        isHeaderVisible = true
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Tampilkan Menu",
                                    tint = BrightOrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Tampilkan Menu & Pencarian",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrightOrangePrimary
                                )
                                if (searchQuery.isNotBlank()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "(\"$searchQuery\")",
                                        fontSize = 11.sp,
                                        color = NeutralMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BrightPeach,
                                    modifier = Modifier.clickable { isHeaderVisible = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.Search, contentDescription = "Cari", tint = BrightOrangeDark, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Cari", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightOrangeDark)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.navigateTo(CafeScreen.CASHIER_SUMMARY) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.AccountBalanceWallet,
                                        contentDescription = "Rekap Kas",
                                        tint = FreshGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Menu Grid with adaptive column sizing
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
                            if (searchQuery.isNotBlank()) {
                                TextButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Text("Reset Pencarian")
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = gridColumns,
                        contentPadding = PaddingValues(if (isCompact) 10.dp else 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("pos_menu_grid")
                    ) {
                        items(menus, key = { it.id }) { menu ->
                            MenuGridCard(
                                menu = menu,
                                currency = currency,
                                isCompact = isCompact,
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
    }

    // Add with notes dialog
    showNotesDialogForMenu?.let { menu ->
        AlertDialog(
            onDismissRequest = { showNotesDialogForMenu = null },
            title = { Text(menu.name, fontWeight = FontWeight.Bold) },
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
    isCompact: Boolean = false,
    onAddToCart: () -> Unit,
    onAddWithNotes: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("menu_card_${menu.id}")
    ) {
        Column {
            // Adaptive Product Image Banner (proportional 1.35 aspect ratio for sharp, uncropped images on any resolution)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
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
                            modifier = Modifier.size(if (isCompact) 34.dp else 42.dp)
                        )
                    }
                }

                // Quick Add overlay button at top-right
                IconButton(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(if (isCompact) 30.dp else 34.dp)
                        .background(BrightOrangePrimary, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Tambah Cepat",
                        tint = Color.White,
                        modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompact) 8.dp else 10.dp)
            ) {
                // Adaptive Product Title: allows 2 lines with clean line height so long names are neatly readable
                Text(
                    text = menu.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeutralDark,
                        fontSize = if (isCompact) 13.5.sp else 14.5.sp,
                        lineHeight = if (isCompact) 17.sp else 19.sp
                    ),
                    maxLines = 2,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (menu.description.isNotBlank()) {
                    Text(
                        text = menu.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = if (isCompact) 10.5.sp else 11.5.sp
                        ),
                        color = NeutralMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(6.dp))

                // Price & Note button row: properly weighted so price never overlaps the Note button
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
                            fontSize = if (isCompact) 12.5.sp else 14.sp
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(Modifier.width(4.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrightPeach,
                        modifier = Modifier.clickable { onAddWithNotes() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = if (isCompact) 6.dp else 8.dp,
                                vertical = 3.5.dp
                            )
                        ) {
                            Icon(
                                Icons.Outlined.EditNote,
                                contentDescription = "Catatan Khusus",
                                tint = BrightOrangeDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "Note",
                                fontSize = if (isCompact) 10.sp else 11.sp,
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
