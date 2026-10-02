package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: CafePosViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val lastTransaction by viewModel.lastTransaction.collectAsState()

    var showCartSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }

    val isKasir = currentUser?.role == "KASIR"

    // Intercept BackHandler for sub-screens
    if (currentScreen != CafeScreen.POS) {
        BackHandler {
            viewModel.navigateTo(CafeScreen.POS)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Navigation Rail on wide screens
            if (isWide) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = CoffeeBrownPrimary
                ) {
                    Spacer(Modifier.height(16.dp))
                    Icon(
                        Icons.Filled.LocalCafe,
                        contentDescription = "Rencana Kopi",
                        tint = CoffeeAmber,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(24.dp))

                    NavigationRailItem(
                        selected = currentScreen == CafeScreen.POS,
                        onClick = { viewModel.navigateTo(CafeScreen.POS) },
                        icon = { Icon(Icons.Filled.PointOfSale, contentDescription = null) },
                        label = { Text("Kasir") }
                    )

                    NavigationRailItem(
                        selected = currentScreen == CafeScreen.PRINTER_SETTINGS,
                        onClick = { viewModel.navigateTo(CafeScreen.PRINTER_SETTINGS) },
                        icon = { Icon(Icons.Filled.Print, contentDescription = null) },
                        label = { Text("Printer") }
                    )

                    if (!isKasir) {
                        NavigationRailItem(
                            selected = currentScreen == CafeScreen.INVENTORY,
                            onClick = { viewModel.navigateTo(CafeScreen.INVENTORY) },
                            icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                            label = { Text("Bahan") }
                        )
                        NavigationRailItem(
                            selected = currentScreen == CafeScreen.MENU_MANAGEMENT,
                            onClick = { viewModel.navigateTo(CafeScreen.MENU_MANAGEMENT) },
                            icon = { Icon(Icons.Filled.RestaurantMenu, contentDescription = null) },
                            label = { Text("Menu") }
                        )
                        NavigationRailItem(
                            selected = currentScreen == CafeScreen.REPORTS,
                            onClick = { viewModel.navigateTo(CafeScreen.REPORTS) },
                            icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                            label = { Text("Laporan") }
                        )
                        NavigationRailItem(
                            selected = currentScreen == CafeScreen.GOOGLE_SHEETS,
                            onClick = { viewModel.navigateTo(CafeScreen.GOOGLE_SHEETS) },
                            icon = { Icon(Icons.Filled.CloudSync, contentDescription = null) },
                            label = { Text("Sheets") }
                        )
                        NavigationRailItem(
                            selected = currentScreen == CafeScreen.SETTINGS,
                            onClick = { viewModel.navigateTo(CafeScreen.SETTINGS) },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text("Setting") }
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    NavigationRailItem(
                        selected = false,
                        onClick = { viewModel.switchUser() },
                        icon = { Icon(Icons.Outlined.Lock, contentDescription = "Ganti Akun") },
                        label = { Text("Kunci") }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Main Screen Content
            Scaffold(
                bottomBar = {
                    if (!isWide) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = CoffeeBrownPrimary
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == CafeScreen.POS,
                                onClick = { viewModel.navigateTo(CafeScreen.POS) },
                                icon = { Icon(Icons.Filled.PointOfSale, contentDescription = null) },
                                label = { Text("Kasir") },
                                modifier = Modifier.testTag("nav_pos")
                            )

                            NavigationBarItem(
                                selected = currentScreen == CafeScreen.PRINTER_SETTINGS,
                                onClick = { viewModel.navigateTo(CafeScreen.PRINTER_SETTINGS) },
                                icon = { Icon(Icons.Filled.Print, contentDescription = null) },
                                label = { Text("Printer") },
                                modifier = Modifier.testTag("nav_printer")
                            )

                            if (isKasir) {
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { viewModel.switchUser() },
                                    icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                    label = { Text("Ganti Akun") },
                                    modifier = Modifier.testTag("nav_switch_user")
                                )
                            } else {
                                NavigationBarItem(
                                    selected = currentScreen == CafeScreen.INVENTORY,
                                    onClick = { viewModel.navigateTo(CafeScreen.INVENTORY) },
                                    icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                                    label = { Text("Bahan") },
                                    modifier = Modifier.testTag("nav_inventory")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == CafeScreen.MENU_MANAGEMENT,
                                    onClick = { viewModel.navigateTo(CafeScreen.MENU_MANAGEMENT) },
                                    icon = { Icon(Icons.Filled.RestaurantMenu, contentDescription = null) },
                                    label = { Text("Menu") },
                                    modifier = Modifier.testTag("nav_menu")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == CafeScreen.REPORTS,
                                    onClick = { viewModel.navigateTo(CafeScreen.REPORTS) },
                                    icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                                    label = { Text("Laporan") },
                                    modifier = Modifier.testTag("nav_reports")
                                )
                                NavigationBarItem(
                                    selected = currentScreen in listOf(
                                        CafeScreen.GOOGLE_SHEETS,
                                        CafeScreen.SETTINGS
                                    ),
                                    onClick = { showMoreMenuSheet = true },
                                    icon = { Icon(Icons.Filled.MoreHoriz, contentDescription = null) },
                                    label = { Text("Lainnya") },
                                    modifier = Modifier.testTag("nav_more")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isKasir && currentScreen !in listOf(CafeScreen.POS, CafeScreen.PRINTER_SETTINGS)) {
                        AccessDeniedScreen(
                            onLoginAdmin = { viewModel.switchUser() }
                        )
                    } else {
                        when (currentScreen) {
                            CafeScreen.POS -> PosScreen(
                                viewModel = viewModel,
                                onOpenCart = { showCartSheet = true }
                            )
                            CafeScreen.INVENTORY -> InventoryScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(CafeScreen.POS) }
                            )
                            CafeScreen.MENU_MANAGEMENT -> MenuManagementScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(CafeScreen.POS) }
                            )
                            CafeScreen.REPORTS -> ReportsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(CafeScreen.POS) }
                            )
                            CafeScreen.PRINTER_SETTINGS -> PrinterSettingsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(CafeScreen.POS) }
                            )
                            CafeScreen.GOOGLE_SHEETS -> GoogleSheetsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(CafeScreen.POS) }
                            )
                            CafeScreen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(CafeScreen.POS) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Cart Modal Bottom Sheet
    if (showCartSheet) {
        CartModalBottomSheet(
            viewModel = viewModel,
            onDismiss = { showCartSheet = false },
            onProceedToPayment = {
                showCartSheet = false
                showPaymentDialog = true
            }
        )
    }

    // Payment Dialog
    if (showPaymentDialog) {
        PaymentDialog(
            viewModel = viewModel,
            onDismiss = { showPaymentDialog = false },
            onPaymentSuccess = {
                showPaymentDialog = false
            }
        )
    }

    // Receipt Dialog after payment
    lastTransaction?.let { completed ->
        ReceiptDialog(
            completed = completed,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }

    // PIN Authentication Dialog when logged out / switching user
    if (currentUser == null) {
        PinLoginDialog(
            viewModel = viewModel,
            onSuccess = { /* Logged in */ }
        )
    }

    // More Options Modal Sheet for Mobile
    if (showMoreMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenuSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Menu Lainnya & Pengaturan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CoffeeBrownDark
                )

                ListItem(
                    headlineContent = { Text("Printer Thermal Bluetooth (ESC/POS)") },
                    supportingContent = { Text("Scan perangkat, sambungkan 58mm/80mm, & test print") },
                    leadingContent = { Icon(Icons.Filled.Print, contentDescription = null, tint = CoffeeBrownPrimary) },
                    modifier = Modifier.clickable {
                        showMoreMenuSheet = false
                        viewModel.navigateTo(CafeScreen.PRINTER_SETTINGS)
                    }
                )

                ListItem(
                    headlineContent = { Text("Integrasi Google Sheets Cloud") },
                    supportingContent = { Text("Sinkronisasi omset, transaksi & stok ke spreadsheet") },
                    leadingContent = { Icon(Icons.Filled.CloudSync, contentDescription = null, tint = CoffeeGreen) },
                    modifier = Modifier.clickable {
                        showMoreMenuSheet = false
                        viewModel.navigateTo(CafeScreen.GOOGLE_SHEETS)
                    }
                )

                ListItem(
                    headlineContent = { Text("Profil Kafe, Pajak PB1 & Backup") },
                    supportingContent = { Text("Nama usaha, alamat, PIN kasir, ekspor/impor database") },
                    leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null, tint = CoffeeAmber) },
                    modifier = Modifier.clickable {
                        showMoreMenuSheet = false
                        viewModel.navigateTo(CafeScreen.SETTINGS)
                    }
                )

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun AccessDeniedScreen(onLoginAdmin: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(CoffeeRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = CoffeeRed,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Akses Terbatas: Khusus Owner / Admin",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CoffeeBrownDark,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Akun Kasir hanya memiliki akses ke modul penjualan POS dan cetak struk Bluetooth.\nFitur ini memerlukan hak akses Owner / Manager.",
                style = MaterialTheme.typography.bodyMedium,
                color = NeutralMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onLoginAdmin,
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Login sebagai Admin (PIN 1234)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

