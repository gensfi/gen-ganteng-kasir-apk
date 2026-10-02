package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val reportSummary by viewModel.reportSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var selectedPeriodDays by remember { mutableStateOf(0) } // 0: Hari Ini, 7: 7 Hari, 30: 30 Hari

    var selectedTransactionDetails by remember { mutableStateOf<Pair<TransactionEntity, List<TransactionItemEntity>>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    LaunchedEffect(selectedPeriodDays) {
        viewModel.loadReports(selectedPeriodDays)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan & Riwayat Penjualan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPeriodDays == 0,
                        onClick = { selectedPeriodDays = 0 },
                        label = { Text("Hari Ini") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPeriodDays == 7,
                        onClick = { selectedPeriodDays = 7 },
                        label = { Text("7 Hari") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPeriodDays == 30,
                        onClick = { selectedPeriodDays = 30 },
                        label = { Text("30 Hari") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Summary KPI Cards
            item {
                reportSummary?.let { rep ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReportCard(
                                title = "Total Omset",
                                value = currency.format(rep.totalRevenue),
                                color = CoffeeBrownPrimary,
                                icon = Icons.Filled.AccountBalanceWallet,
                                modifier = Modifier.weight(1f)
                            )
                            ReportCard(
                                title = "Laba Bersih (Net)",
                                value = currency.format(rep.totalNetProfit),
                                color = CoffeeGreen,
                                icon = Icons.Filled.TrendingUp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReportCard(
                                title = "Total Transaksi",
                                value = "${rep.transactionCount} Pesanan",
                                color = CoffeeAmber,
                                icon = Icons.Filled.ReceiptLong,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Top Selling Menu
            item {
                reportSummary?.topSellingItems?.let { topItems ->
                    if (topItems.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "🔥 Menu Terlaris (Top Selling)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = CoffeeBrownDark
                                )
                                Spacer(Modifier.height(8.dp))
                                topItems.forEachIndexed { idx, (menuName, qty) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${idx + 1}. $menuName", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("$qty Porsi", fontSize = 13.sp, color = CoffeeAmber, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ingredients used
            item {
                reportSummary?.ingredientUsageSummary?.let { usage ->
                    if (usage.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "📦 Penggunaan Bahan Baku Periode Ini",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = CoffeeBrownDark
                                )
                                Spacer(Modifier.height(8.dp))
                                usage.forEach { (ingName, qtyStr) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(ingName, fontSize = 13.sp)
                                        Text(qtyStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CoffeeBrownPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Transaction History Header
            item {
                Text(
                    "Riwayat Transaksi Terbaru (${transactions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CoffeeBrownDark
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text("Belum ada riwayat transaksi.", color = NeutralMedium, fontSize = 13.sp)
                }
            } else {
                items(transactions) { trx ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val items = viewModel.repository.getTransactionItems(trx.id)
                                    selectedTransactionDetails = trx to items
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(trx.id, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(dateFormat.format(Date(trx.timestamp)), fontSize = 11.sp, color = NeutralMedium)
                                Text("Kasir: ${trx.cashierName} • ${trx.paymentMethod}", fontSize = 11.sp, color = NeutralMedium)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    currency.format(trx.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = CoffeeBrownPrimary
                                )
                                if (trx.isSyncedToSheets) {
                                    Text("✓ Google Sheets", fontSize = 10.sp, color = CoffeeGreen, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("○ Offline Local", fontSize = 10.sp, color = CoffeeAmber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Transaction Details Dialog
    selectedTransactionDetails?.let { (trx, items) ->
        AlertDialog(
            onDismissRequest = { selectedTransactionDetails = null },
            title = { Text("Detail: ${trx.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Waktu: ${dateFormat.format(Date(trx.timestamp))}", fontSize = 12.sp)
                    Text("Kasir: ${trx.cashierName}", fontSize = 12.sp)
                    if (!trx.customerName.isNullOrBlank()) {
                        Text("Pelanggan: ${trx.customerName}", fontSize = 12.sp)
                    }
                    Divider(Modifier.padding(vertical = 4.dp))
                    items.forEach { itm ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${itm.quantity}x ${itm.menuName}", fontSize = 13.sp)
                            Text(currency.format(itm.price * itm.quantity), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Divider(Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Tagihan", fontWeight = FontWeight.Bold)
                        Text(currency.format(trx.totalAmount), fontWeight = FontWeight.Bold, color = CoffeeBrownPrimary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTransactionDetails = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun ReportCard(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, fontSize = 12.sp, color = NeutralMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
