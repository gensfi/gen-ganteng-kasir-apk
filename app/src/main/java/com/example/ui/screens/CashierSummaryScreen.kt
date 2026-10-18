package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierSummaryScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val allTransactions by viewModel.repository.getAllTransactions().collectAsState(initial = emptyList())

    var filterPeriod by remember { mutableStateOf("TODAY") } // TODAY, ALL_MINE, ALL_CASHIERS
    var selectedTrxForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var isGeneratingShiftPdf by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    // Start of Today timestamp
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Filter transactions based on selection
    val filteredTransactions = remember(allTransactions, filterPeriod, currentUser) {
        allTransactions.filter { trx ->
            when (filterPeriod) {
                "TODAY" -> {
                    val isToday = trx.timestamp >= startOfToday
                    val isUser = currentUser == null || currentUser?.role == "ADMIN" || trx.cashierId == currentUser?.id
                    isToday && isUser
                }
                "ALL_MINE" -> {
                    currentUser == null || trx.cashierId == currentUser?.id
                }
                "ALL_CASHIERS" -> true
                else -> true
            }
        }
    }

    // Calculations
    val totalOrders = filteredTransactions.size
    val totalMoneyReceived = filteredTransactions.sumOf { it.totalAmount }
    val averageTicket = if (totalOrders > 0) totalMoneyReceived / totalOrders else 0.0

    // Payment breakdowns
    val cashTransactions = filteredTransactions.filter { it.paymentMethod.equals("CASH", ignoreCase = true) }
    val cashTotal = cashTransactions.sumOf { it.totalAmount }

    val qrisTransactions = filteredTransactions.filter { it.paymentMethod.equals("QRIS", ignoreCase = true) }
    val qrisTotal = qrisTransactions.sumOf { it.totalAmount }

    val onlineTransactions = filteredTransactions.filter { it.paymentMethod.equals("ONLINE", true) || it.paymentMethod.equals("PESANAN_ONLINE", true) || it.paymentMethod.equals("TRANSFER", true) }
    val onlineTotal = onlineTransactions.sumOf { it.totalAmount }

    val generateShiftPdfFile: suspend () -> File? = {
        val cafeName = viewModel.repository.getSetting("cafe_name") ?: "Rencana Kopi"
        val cafeAddress = viewModel.repository.getSetting("cafe_address") ?: "Jl. Kopi Nikmat No. 1"
        val cafePhone = viewModel.repository.getSetting("cafe_phone") ?: "0812-3456-7890"

        val allItems = withContext(Dispatchers.IO) {
            viewModel.repository.getAllTransactionItems()
        }
        val trxIds = filteredTransactions.map { it.id }.toSet()
        val shiftItems = allItems.filter { it.transactionId in trxIds }

        val pdfData = PdfReportGenerator.PdfShiftReportData(
            cafeName = cafeName,
            cafeAddress = cafeAddress,
            cafePhone = cafePhone,
            cashierName = currentUser?.name ?: "Kasir",
            cashierRole = currentUser?.role ?: "KASIR",
            shiftDateFormatted = dateFormat.format(Date()),
            printedTimeFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date()),
            transactions = filteredTransactions,
            items = shiftItems,
            totalRevenue = totalMoneyReceived,
            cashTotal = cashTotal,
            qrisTotal = qrisTotal,
            onlineTotal = onlineTotal,
            averageTicket = averageTicket
        )
        PdfReportGenerator.generateCashierShiftPdf(context, pdfData)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Rekap Order & Kas Kasir",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${currentUser?.name ?: "Kasir"} (${currentUser?.role ?: "KASIR"}) • ${dateFormat.format(Date())}",
                            fontSize = 11.5.sp,
                            color = NeutralMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Kasir")
                    }
                },
                actions = {
                    // Export PDF Shift Report
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    isGeneratingShiftPdf = true
                                    val file = generateShiftPdfFile()
                                    if (file != null) {
                                        PdfReportGenerator.openPdf(context, file)
                                    } else {
                                        Toast.makeText(context, "Gagal membuat PDF Laporan Shift", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isGeneratingShiftPdf = false
                                }
                            }
                        },
                        enabled = !isGeneratingShiftPdf,
                        modifier = Modifier.testTag("btn_export_shift_pdf")
                    ) {
                        if (isGeneratingShiftPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Ekspor PDF Shift", tint = BrightRed)
                        }
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                // Print Cashier Shift Report
                                val reportText = buildString {
                                    appendLine("=== REKAP SHIFT KASIR ===")
                                    appendLine("Rencana Kopi POS")
                                    appendLine("Tanggal: ${dateFormat.format(Date())} ${timeFormat.format(Date())}")
                                    appendLine("Kasir  : ${currentUser?.name ?: "-"}")
                                    appendLine("--------------------------------")
                                    appendLine("Total Order : $totalOrders Transaksi")
                                    appendLine("Total Uang  : ${currency.format(totalMoneyReceived)}")
                                    appendLine("--------------------------------")
                                    appendLine("RINCIAN METODE BAYAR:")
                                    appendLine("Tunai/Cash     : ${currency.format(cashTotal)} (${cashTransactions.size}x)")
                                    appendLine("QRIS           : ${currency.format(qrisTotal)} (${qrisTransactions.size}x)")
                                    appendLine("Pesanan Online : ${currency.format(onlineTotal)} (${onlineTransactions.size}x)")
                                    appendLine("--------------------------------")
                                    appendLine("Kas Laci Fisik : ${currency.format(cashTotal)}")
                                    appendLine("================================")
                                    appendLine("Tanda Tangan Kasir:\n\n\n( ${currentUser?.name ?: "Kasir"} )")
                                }
                                viewModel.printCustomReceipt(reportText)
                                Toast.makeText(context, "Mencetak Rekap Kasir ke Printer Bluetooth...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("btn_print_cashier_summary")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Cetak Rekap Kasir", tint = BrightOrangePrimary)
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
            // Period Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterPeriod == "TODAY",
                        onClick = { filterPeriod = "TODAY" },
                        label = { Text("Hari Ini (Shift Ini)") },
                        leadingIcon = {
                            if (filterPeriod == "TODAY") {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                    FilterChip(
                        selected = filterPeriod == "ALL_MINE",
                        onClick = { filterPeriod = "ALL_MINE" },
                        label = { Text("Semua Order Saya") }
                    )
                    if (currentUser?.role == "ADMIN") {
                        FilterChip(
                            selected = filterPeriod == "ALL_CASHIERS",
                            onClick = { filterPeriod = "ALL_CASHIERS" },
                            label = { Text("Semua Kasir") }
                        )
                    }
                }
            }

            // Hero Metric Cards (Total Uang & Total Order)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrightPeach),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "TOTAL UANG DIDAPAT (OMSET)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightOrangeDark
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currency.format(totalMoneyReceived),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrightOrangeDark
                        )

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = BrightOrangePrimary.copy(alpha = 0.25f))
                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Order Selesai",
                                    fontSize = 11.sp,
                                    color = NeutralMedium
                                )
                                Text(
                                    text = "$totalOrders Transaksi",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeutralDark
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Rata-rata per Transaksi",
                                    fontSize = 11.sp,
                                    color = NeutralMedium
                                )
                                Text(
                                    text = currency.format(averageTicket),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeutralDark
                                )
                            }
                        }
                    }
                }
            }

            // Breakdown by Payment Method
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = BrightOrangePrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Rincian Metode Pembayaran", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        // 1. Tunai (Uang Fisik Kasir)
                        PaymentMethodRow(
                            icon = Icons.Filled.Money,
                            iconColor = FreshGreen,
                            title = "Uang Tunai (Cash di Laci)",
                            subtitle = "${cashTransactions.size} Transaksi • Wajib ada di kasir",
                            amount = currency.format(cashTotal),
                            containerColor = FreshGreenLight
                        )

                        // 2. QRIS (Digital)
                        PaymentMethodRow(
                            icon = Icons.Filled.QrCode2,
                            iconColor = BrightOrangePrimary,
                            title = "QRIS (GoPay/Shopee/BCA/OVO)",
                            subtitle = "${qrisTransactions.size} Transaksi • Masuk rekening digital",
                            amount = currency.format(qrisTotal),
                            containerColor = BrightPeach
                        )

                        // 3. Pesanan Online
                        PaymentMethodRow(
                            icon = Icons.Filled.ShoppingBag,
                            iconColor = BrightTeal,
                            title = "Pesanan Online",
                            subtitle = "${onlineTransactions.size} Transaksi • GoFood/Grab/Shopee",
                            amount = currency.format(onlineTotal),
                            containerColor = BrightTealLight
                        )
                    }
                }
            }

            // Quick Actions: PDF Shift Report, Print Bluetooth, Close Shift
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Aksi Selesai Shift & Laporan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NeutralDark
                        )

                        // Row 1: Generate PDF Shift Report & Share PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            isGeneratingShiftPdf = true
                                            val file = generateShiftPdfFile()
                                            if (file != null) {
                                                PdfReportGenerator.openPdf(context, file)
                                            } else {
                                                Toast.makeText(context, "Gagal membuat PDF Laporan Shift", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingShiftPdf = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightOrangePrimary),
                                enabled = !isGeneratingShiftPdf,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("btn_export_pdf_shift_main")
                            ) {
                                if (isGeneratingShiftPdf) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("Laporan PDF Shift", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            isGeneratingShiftPdf = true
                                            val file = generateShiftPdfFile()
                                            if (file != null) {
                                                PdfReportGenerator.sharePdf(context, file)
                                            } else {
                                                Toast.makeText(context, "Gagal menyiapkan file PDF", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingShiftPdf = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isGeneratingShiftPdf,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .testTag("btn_share_pdf_shift")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Bagikan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Row 2: Print Thermal Receipt & Close Shift
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val reportText = buildString {
                                            appendLine("=== REKAP SHIFT KASIR ===")
                                            appendLine("Rencana Kopi POS")
                                            appendLine("Tanggal: ${dateFormat.format(Date())} ${timeFormat.format(Date())}")
                                            appendLine("Kasir  : ${currentUser?.name ?: "-"}")
                                            appendLine("--------------------------------")
                                            appendLine("Total Order : $totalOrders Transaksi")
                                            appendLine("Total Uang  : ${currency.format(totalMoneyReceived)}")
                                            appendLine("--------------------------------")
                                            appendLine("RINCIAN METODE BAYAR:")
                                            appendLine("Tunai/Cash     : ${currency.format(cashTotal)} (${cashTransactions.size}x)")
                                            appendLine("QRIS           : ${currency.format(qrisTotal)} (${qrisTransactions.size}x)")
                                            appendLine("Pesanan Online : ${currency.format(onlineTotal)} (${onlineTransactions.size}x)")
                                            appendLine("--------------------------------")
                                            appendLine("Kas Laci Fisik : ${currency.format(cashTotal)}")
                                            appendLine("================================")
                                        }
                                        viewModel.printCustomReceipt(reportText)
                                        Toast.makeText(context, "Mencetak Struk Rekap Kasir...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_print_shift_bottom")
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cetak Struk", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                            }

                            Button(
                                onClick = {
                                    showCloseShiftDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_tutup_shift")
                            ) {
                                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Tutup Shift", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }

            // Transaction History Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Lengkap Transaksi ($totalOrders)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NeutralDark
                    )
                }
            }

            // Empty state
            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = NeutralMedium,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Belum ada transaksi di periode ini",
                                fontWeight = FontWeight.SemiBold,
                                color = NeutralMedium
                            )
                        }
                    }
                }
            }

            // Transactions list
            items(filteredTransactions, key = { it.id }) { trx ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTrxForDetail = trx }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (trx.paymentMethod.uppercase()) {
                                            "CASH" -> FreshGreenLight
                                            "QRIS" -> BrightPeach
                                            else -> BrightTealLight
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (trx.paymentMethod.uppercase()) {
                                        "CASH" -> Icons.Filled.Money
                                        "QRIS" -> Icons.Filled.QrCode2
                                        else -> Icons.Filled.AccountBalance
                                    },
                                    contentDescription = null,
                                    tint = when (trx.paymentMethod.uppercase()) {
                                        "CASH" -> FreshGreen
                                        "QRIS" -> BrightOrangePrimary
                                        else -> BrightTeal
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = trx.id,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = NeutralDark
                                )
                                Text(
                                    text = "${timeFormat.format(Date(trx.timestamp))} • ${trx.cashierName}${if (!trx.customerName.isNullOrBlank()) " • ${trx.customerName}" else ""}",
                                    fontSize = 11.sp,
                                    color = NeutralMedium
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = currency.format(trx.totalAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NeutralDark
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (trx.paymentMethod.uppercase()) {
                                    "CASH" -> FreshGreenLight
                                    "QRIS" -> BrightPeach
                                    else -> BrightTealLight
                                }
                            ) {
                                Text(
                                    text = trx.paymentMethod.uppercase(),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (trx.paymentMethod.uppercase()) {
                                        "CASH" -> FreshGreen
                                        "QRIS" -> BrightOrangeDark
                                        else -> BrightTeal
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Receipt Modal
    selectedTrxForDetail?.let { trx ->
        var items by remember { mutableStateOf<List<com.example.data.local.entity.TransactionItemEntity>>(emptyList()) }
        LaunchedEffect(trx.id) {
            items = viewModel.repository.getTransactionItems(trx.id)
        }

        AlertDialog(
            onDismissRequest = { selectedTrxForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Receipt, contentDescription = null, tint = BrightOrangePrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Detail Transaksi ${trx.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Waktu: ${dateFormat.format(Date(trx.timestamp))} ${timeFormat.format(Date(trx.timestamp))}", fontSize = 12.sp, color = NeutralMedium)
                    Text("Kasir: ${trx.cashierName} • Metode: ${trx.paymentMethod}", fontSize = 12.sp, color = NeutralMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.quantity}x ${item.menuName}", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            Text(currency.format(item.subtotal), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontSize = 12.sp)
                        Text(currency.format(trx.subtotal), fontSize = 12.sp)
                    }
                    if (trx.taxAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pajak", fontSize = 12.sp)
                            Text(currency.format(trx.taxAmount), fontSize = 12.sp)
                        }
                    }
                    if (trx.serviceChargeAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Biaya Layanan", fontSize = 12.sp)
                            Text(currency.format(trx.serviceChargeAmount), fontSize = 12.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL AKHIR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(currency.format(trx.totalAmount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrightOrangeDark)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.printSingleTransaction(trx, items) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightOrangePrimary)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cetak Struk")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTrxForDetail = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Close Shift Confirmation & PDF Export Dialog
    if (showCloseShiftDialog) {
        AlertDialog(
            onDismissRequest = { showCloseShiftDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = BrightOrangePrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Tutup Shift & Rekap Kasir", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pastikan uang fisik di laci kasir dan catatan transaksi sudah sesuai sebelum menutup shift.",
                        fontSize = 12.5.sp,
                        color = NeutralMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FreshGreenLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("UANG FISIK KAS LACI WAJIB ADA:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FreshGreen)
                            Text(currency.format(cashTotal), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = FreshGreen)
                            Text("${cashTransactions.size} Transaksi Tunai", fontSize = 11.sp, color = NeutralMedium)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeutralLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Omset", fontSize = 11.sp, color = NeutralMedium)
                                Text(currency.format(totalMoneyReceived), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Order", fontSize = 11.sp, color = NeutralMedium)
                                Text("$totalOrders Order", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Pilih aksi laporan shift Anda:", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)

                    // Action: Open PDF
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    isGeneratingShiftPdf = true
                                    val file = generateShiftPdfFile()
                                    if (file != null) {
                                        PdfReportGenerator.openPdf(context, file)
                                    } else {
                                        Toast.makeText(context, "Gagal membuat PDF Laporan Shift", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isGeneratingShiftPdf = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrightOrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("📄 Buka Laporan PDF Shift", fontWeight = FontWeight.Bold)
                    }

                    // Action: Share PDF
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    isGeneratingShiftPdf = true
                                    val file = generateShiftPdfFile()
                                    if (file != null) {
                                        PdfReportGenerator.sharePdf(context, file)
                                    } else {
                                        Toast.makeText(context, "Gagal menyiapkan file PDF", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isGeneratingShiftPdf = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("📲 Bagikan PDF (WhatsApp)", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCloseShiftDialog = false
                        viewModel.switchUser()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tutup Shift & Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseShiftDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun PaymentMethodRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    amount: String,
    containerColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeutralDark)
                    Text(subtitle, fontSize = 10.5.sp, color = NeutralMedium)
                }
            }
            Text(amount, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = NeutralDark)
        }
    }
}
