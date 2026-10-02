package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.IngredientEntity
import com.example.data.local.entity.MenuEntity
import com.example.data.local.entity.RecipeEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import com.example.util.ImageUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val menus by viewModel.menus.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Menu & Resep, 1: Kategori
    var showMenuDialog by remember { mutableStateOf<MenuEntity?>(null) }
    var isNewMenu by remember { mutableStateOf(false) }

    var showCategoryDialog by remember { mutableStateOf<CategoryEntity?>(null) }
    var isNewCategory by remember { mutableStateOf(false) }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Menu & Resep", fontWeight = FontWeight.Bold) },
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
                        if (selectedTab == 0) {
                            val defaultCatId = categories.firstOrNull()?.id ?: 1L
                            isNewMenu = true
                            showMenuDialog = MenuEntity(
                                id = 0,
                                categoryId = defaultCatId,
                                name = "",
                                description = "",
                                price = 20000.0,
                                costPrice = 0.0
                            )
                        } else {
                            isNewCategory = true
                            showCategoryDialog = CategoryEntity(id = 0, name = "", iconName = "Coffee")
                        }
                    },
                    containerColor = CoffeeBrownPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_menu_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CoffeeBrownPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Daftar Menu & Resep (${menus.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Kategori Menu (${categories.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // Menus list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(menus) { menu ->
                        val cat = categories.find { it.id == menu.categoryId }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail Photo
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NeutralLight)
                                ) {
                                    if (!menu.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = menu.imageUrl,
                                            contentDescription = menu.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.LocalCafe,
                                                contentDescription = null,
                                                tint = CoffeeBrownPrimary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            menu.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = NeutralLight
                                        ) {
                                            Text(
                                                cat?.name ?: "Kategori",
                                                fontSize = 11.sp,
                                                color = NeutralMedium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (menu.description.isNotBlank()) {
                                        Text(
                                            menu.description,
                                            fontSize = 12.sp,
                                            color = NeutralMedium,
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            maxLines = 1
                                        )
                                    }

                                    Row(modifier = Modifier.padding(top = 4.dp)) {
                                        Text(
                                            "Jual: ${currency.format(menu.price)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CoffeeBrownPrimary
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "HPP: ${currency.format(menu.costPrice)}",
                                            fontSize = 13.sp,
                                            color = CoffeeAmber
                                        )
                                    }
                                }

                                if (currentUser?.role == "ADMIN") {
                                    Row {
                                        IconButton(onClick = {
                                            isNewMenu = false
                                            showMenuDialog = menu
                                        }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = CoffeeAmber)
                                        }
                                        IconButton(onClick = {
                                            viewModel.deleteMenu(menu) {}
                                        }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = CoffeeRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Categories list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categories) { cat ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = when (cat.iconName) {
                                        "Coffee" -> Icons.Filled.LocalCafe
                                        "Tea" -> Icons.Filled.EmojiFoodBeverage
                                        "Bakery" -> Icons.Filled.BakeryDining
                                        "Food" -> Icons.Filled.Fastfood
                                        else -> Icons.Filled.Category
                                    }
                                    Icon(icon, contentDescription = null, tint = CoffeeBrownPrimary)
                                    Spacer(Modifier.width(12.dp))
                                    Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                if (currentUser?.role == "ADMIN") {
                                    Row {
                                        IconButton(onClick = {
                                            isNewCategory = false
                                            showCategoryDialog = cat
                                        }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = CoffeeAmber)
                                        }
                                        IconButton(onClick = {
                                            viewModel.deleteCategory(cat) {}
                                        }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = CoffeeRed)
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

    // Menu & Recipe Composer Dialog
    showMenuDialog?.let { initialMenu ->
        MenuRecipeDialog(
            menu = initialMenu,
            categories = categories,
            ingredients = ingredients,
            viewModel = viewModel,
            isNew = isNewMenu,
            onDismiss = { showMenuDialog = null }
        )
    }

    // Category Add/Edit Dialog
    showCategoryDialog?.let { cat ->
        var name by remember { mutableStateOf(cat.name) }
        var iconName by remember { mutableStateOf(cat.iconName) }

        AlertDialog(
            onDismissRequest = { showCategoryDialog = null },
            title = { Text(if (isNewCategory) "Tambah Kategori Baru" else "Edit Kategori") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Kategori") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Pilih Ikon:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Coffee", "Tea", "Bakery", "Food").forEach { ic ->
                            FilterChip(
                                selected = iconName == ic,
                                onClick = { iconName = ic },
                                label = { Text(ic) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCategory(cat.copy(name = name.trim(), iconName = iconName)) {
                            showCategoryDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun MenuRecipeDialog(
    menu: MenuEntity,
    categories: List<CategoryEntity>,
    ingredients: List<IngredientEntity>,
    viewModel: CafePosViewModel,
    isNew: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(menu.name) }
    var description by remember { mutableStateOf(menu.description) }
    var priceText by remember { mutableStateOf(menu.price.toLong().toString()) }
    var selectedCatId by remember { mutableStateOf(menu.categoryId) }
    var imageUrl by remember { mutableStateOf(menu.imageUrl ?: "") }
    var showPresetDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = ImageUtils.saveImageFromUri(context, uri)
            if (localPath != null) {
                imageUrl = localPath
            }
        }
    }

    // Recipes linked to this menu
    var currentRecipes by remember { mutableStateOf<List<RecipeEntity>>(emptyList()) }
    var selectedIngredientToAdd by remember { mutableStateOf<IngredientEntity?>(null) }
    var ingredientAmountText by remember { mutableStateOf("10") }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    LaunchedEffect(menu.id) {
        if (menu.id != 0L) {
            currentRecipes = viewModel.repository.getRecipesForMenu(menu.id)
        }
    }

    // Auto-calculate HPP (Cost Price) from recipe
    val calculatedCostPrice = remember(currentRecipes, ingredients) {
        currentRecipes.sumOf { rec ->
            val ing = ingredients.find { it.id == rec.ingredientId }
            (ing?.costPerUnit ?: 0.0) * rec.amountRequired
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Tambah Menu Baru" else "Edit Menu & Resep") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Photo Upload Card
                item {
                    Text("Foto Produk:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoffeeBrownDark)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Live Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Foto Produk",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = NeutralMedium,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            "Belum ada foto produk",
                                            fontSize = 12.sp,
                                            color = NeutralMedium,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Action Buttons: Galeri & Preset
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Upload HP", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { showPresetDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.Collections, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Preset Kafe", fontSize = 11.sp)
                                }

                                if (imageUrl.isNotBlank()) {
                                    IconButton(
                                        onClick = { imageUrl = "" },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Hapus Foto", tint = CoffeeRed)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Menu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi Singkat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Jual (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Kategori:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(cat.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Resep Bahan Baku (Pengurangan Stok Otomatis):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CoffeeBrownDark
                    )
                    Text(
                        "Setiap 1 pesanan menu ini akan memotong stok bahan baku berikut:",
                        fontSize = 11.sp,
                        color = NeutralMedium
                    )
                }

                // Existing recipes list
                items(currentRecipes) { recipe ->
                    val ing = ingredients.find { it.id == recipe.ingredientId }
                    Surface(
                        color = NeutralLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${ing?.name ?: "Bahan"}: ${recipe.amountRequired} ${ing?.unit ?: ""}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = {
                                currentRecipes = currentRecipes.filter { it != recipe }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Hapus Resep", tint = CoffeeRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Add ingredient to recipe section
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Tambah Bahan ke Resep:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var expanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.weight(1.5f)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    selectedIngredientToAdd?.name ?: "Pilih Bahan",
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                ingredients.forEach { ing ->
                                    DropdownMenuItem(
                                        text = { Text("${ing.name} (${ing.unit})") },
                                        onClick = {
                                            selectedIngredientToAdd = ing
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = ingredientAmountText,
                            onValueChange = { ingredientAmountText = it },
                            placeholder = { Text("Jumlah") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        FilledIconButton(
                            onClick = {
                                val ing = selectedIngredientToAdd
                                val amt = ingredientAmountText.toDoubleOrNull() ?: 0.0
                                if (ing != null && amt > 0) {
                                    currentRecipes = currentRecipes + RecipeEntity(
                                        menuId = menu.id,
                                        ingredientId = ing.id,
                                        amountRequired = amt
                                    )
                                    selectedIngredientToAdd = null
                                    ingredientAmountText = "10"
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = CoffeeBrownPrimary)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Tambah Bahan", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = CoffeeGreenLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimasi HPP (Modal):", fontSize = 12.sp, color = CoffeeGreen, fontWeight = FontWeight.SemiBold)
                            Text(currency.format(calculatedCostPrice), fontSize = 13.sp, color = CoffeeGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedMenu = menu.copy(
                        categoryId = selectedCatId,
                        name = name.trim(),
                        description = description.trim(),
                        price = priceText.toDoubleOrNull() ?: 0.0,
                        costPrice = calculatedCostPrice,
                        imageUrl = imageUrl.trim().ifBlank { null }
                    )
                    viewModel.saveMenu(updatedMenu, currentRecipes) {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
            ) {
                Text("Simpan Menu & Resep")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    // Preset Photo Selection Dialog
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("Pilih Foto Preset Kafe", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ImageUtils.PRESET_PHOTOS) { preset ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    imageUrl = preset.url
                                    showPresetDialog = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                AsyncImage(
                                    model = preset.url,
                                    contentDescription = preset.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    preset.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = CoffeeBrownDark
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}
