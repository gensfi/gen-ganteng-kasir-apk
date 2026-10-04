package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSheetsScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val allTransactions by viewModel.repository.getAllTransactions().collectAsState(initial = emptyList())

    var webhookUrl by remember { mutableStateOf("") }
    var spreadsheetId by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var autoSync by remember { mutableStateOf(false) }
    var lastSyncTime by remember { mutableStateOf("-") }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    // Date range picker state
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    var rangePreset by remember { mutableStateOf("TODAY") }
    var startDateMillis by remember { mutableStateOf(todayStart) }
    var endDateMillis by remember { mutableStateOf(todayEnd) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")) }
    val displayDateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    fun pickDate(isStart: Boolean) {
        val currentMillis = if (isStart) startDateMillis else endDateMillis
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                    if (isStart) {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    } else {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                }
                if (isStart) {
                    startDateMillis = selectedCal.timeInMillis
                    if (startDateMillis > endDateMillis) {
                        endDateMillis = startDateMillis + (24 * 60 * 60 * 1000L - 1)
                    }
                } else {
                    endDateMillis = selectedCal.timeInMillis
                    if (endDateMillis < startDateMillis) {
                        startDateMillis = endDateMillis - (24 * 60 * 60 * 1000L - 1)
                    }
                }
                rangePreset = "CUSTOM"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun applyPreset(preset: String) {
        rangePreset = preset
        when (preset) {
            "TODAY" -> {
                startDateMillis = todayStart
                endDateMillis = todayEnd
            }
            "7DAYS" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                startDateMillis = cal.timeInMillis
                endDateMillis = todayEnd
            }
            "30DAYS" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                startDateMillis = cal.timeInMillis
                endDateMillis = todayEnd
            }
            "THIS_MONTH" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                startDateMillis = cal.timeInMillis
                endDateMillis = todayEnd
            }
        }
    }

    // Filter transactions in range
    val selectedTransactions = remember(allTransactions, startDateMillis, endDateMillis) {
        allTransactions.filter { it.timestamp in startDateMillis..endDateMillis }
    }
    val totalRevenue = selectedTransactions.sumOf { it.totalAmount }
    val cashTotal = selectedTransactions.filter { it.paymentMethod.equals("CASH", true) }.sumOf { it.totalAmount }
    val qrisTotal = selectedTransactions.filter { it.paymentMethod.equals("QRIS", true) }.sumOf { it.totalAmount }
    val transferTotal = selectedTransactions.filter { it.paymentMethod.equals("TRANSFER", true) }.sumOf { it.totalAmount }

    fun generatePdfReport(openAfter: Boolean) {
        coroutineScope.launch {
            isGeneratingPdf = true
            try {
                val cafeName = viewModel.repository.getSetting("cafe_name") ?: "Rencana Kopi"
                val cafeAddress = viewModel.repository.getSetting("cafe_address") ?: ""
                val cafePhone = viewModel.repository.getSetting("cafe_phone") ?: ""

                val allItems = viewModel.repository.getAllTransactionItems()
                val trxIds = selectedTransactions.map { it.id }.toSet()
                val filteredItems = allItems.filter { it.transactionId in trxIds }

                val totalCost = filteredItems.sumOf { it.costPrice * it.quantity }
                val netProfit = totalRevenue - totalCost

                val reportData = PdfReportGenerator.PdfReportData(
                    cafeName = cafeName,
                    cafeAddress = cafeAddress,
                    cafePhone = cafePhone,
                    startDateFormatted = displayDateFormatter.format(Date(startDateMillis)),
                    endDateFormatted = displayDateFormatter.format(Date(endDateMillis)),
                    transactions = selectedTransactions,
                    items = filteredItems,
                    totalRevenue = totalRevenue,
                    totalNetProfit = netProfit,
                    cashTotal = cashTotal,
                    qrisTotal = qrisTotal,
                    transferTotal = transferTotal
                )

                val file = PdfReportGenerator.generateSalesPdf(context, reportData)
                isGeneratingPdf = false

                if (file != null && file.exists()) {
                    Toast.makeText(context, "Laporan PDF berhasil dibuat (${file.name})", Toast.LENGTH_SHORT).show()
                    if (openAfter) {
                        PdfReportGenerator.openPdf(context, file)
                    } else {
                        PdfReportGenerator.sharePdf(context, file)
                    }
                } else {
                    Toast.makeText(context, "Gagal membuat file PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isGeneratingPdf = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        webhookUrl = viewModel.repository.getSetting("google_sheets_url").orEmpty()
        spreadsheetId = viewModel.repository.getSetting("google_sheets_id").orEmpty()
        apiKey = viewModel.repository.getSetting("google_sheets_api_key").orEmpty()
        autoSync = viewModel.repository.getSetting("auto_sync_sheets")?.toBoolean() ?: false
        lastSyncTime = viewModel.repository.getSetting("last_sync_time") ?: "-"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Integrasi Google Sheets", fontWeight = FontWeight.Bold) },
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
            // Hero Intro Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CoffeeGreenLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CloudSync,
                            contentDescription = null,
                            tint = CoffeeGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Dashboard Cloud untuk Owner",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CoffeeBrownDark
                            )
                            Text(
                                "Laporan transaksi, omset, dan stok otomatis masuk ke Google Spreadsheet Anda secara real-time.",
                                fontSize = 12.sp,
                                color = CoffeeBrownDark.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Rekap Penjualan & Ekspor Laporan PDF dengan Pilihan Tanggal
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BrightPeach),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = BrightOrangePrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Rekap Penjualan & Ekspor File PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NeutralDark)
                                Text("Pilih rentang tanggal untuk unduh / cetak laporan PDF resmi", fontSize = 11.5.sp, color = NeutralMedium)
                            }
                        }

                        // Presets Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = rangePreset == "TODAY",
                                onClick = { applyPreset("TODAY") },
                                label = { Text("Hari Ini", fontSize = 11.5.sp) }
                            )
                            FilterChip(
                                selected = rangePreset == "7DAYS",
                                onClick = { applyPreset("7DAYS") },
                                label = { Text("7 Hari", fontSize = 11.5.sp) }
                            )
                            FilterChip(
                                selected = rangePreset == "30DAYS",
                                onClick = { applyPreset("30DAYS") },
                                label = { Text("30 Hari", fontSize = 11.5.sp) }
                            )
                            FilterChip(
                                selected = rangePreset == "THIS_MONTH",
                                onClick = { applyPreset("THIS_MONTH") },
                                label = { Text("Bulan Ini", fontSize = 11.5.sp) }
                            )
                        }

                        // Date Pickers Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Start Date Picker Field
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NeutralLight,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pickDate(isStart = true) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = BrightOrangePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text("Dari Tanggal", fontSize = 10.sp, color = NeutralMedium)
                                        Text(dateFormatter.format(Date(startDateMillis)), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = NeutralDark)
                                    }
                                }
                            }

                            // End Date Picker Field
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NeutralLight,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pickDate(isStart = false) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = BrightOrangePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text("Sampai Tanggal", fontSize = 10.sp, color = NeutralMedium)
                                        Text(dateFormatter.format(Date(endDateMillis)), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = NeutralDark)
                                    }
                                }
                            }
                        }

                        // Live Period Summary Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BrightPeach.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Omset (${selectedTransactions.size} Transaksi)", fontSize = 11.sp, color = BrightOrangeDark)
                                        Text(currency.format(totalRevenue), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrightOrangeDark)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White
                                    ) {
                                        Text(
                                            text = "${selectedTransactions.size} Order",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrightOrangeDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Rincian: Tunai ${currency.format(cashTotal)} • QRIS ${currency.format(qrisTotal)} • Transfer ${currency.format(transferTotal)}",
                                    fontSize = 10.5.sp,
                                    color = NeutralMedium
                                )
                            }
                        }

                        // Action Buttons: Open PDF & Share PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { generatePdfReport(openAfter = true) },
                                enabled = !isGeneratingPdf,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightOrangePrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_generate_open_pdf")
                            ) {
                                if (isGeneratingPdf) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Buka File PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { generatePdfReport(openAfter = false) },
                                enabled = !isGeneratingPdf,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_share_pdf")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = BrightOrangePrimary)
                                Spacer(Modifier.width(6.dp))
                                Text("Bagikan PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrightOrangePrimary)
                            }
                        }
                    }
                }
            }

            // Sync Status & Trigger Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Status Sinkronisasi:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(syncState, fontSize = 13.sp, color = CoffeeAmber, fontWeight = FontWeight.SemiBold)
                        Text("Terakhir sinkronisasi: $lastSyncTime", fontSize = 11.sp, color = NeutralMedium)

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.triggerGoogleSheetsSync { success, msg ->
                                    coroutineScope.launch {
                                        lastSyncTime = viewModel.repository.getSetting("last_sync_time") ?: "-"
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sync_now_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sinkronkan Transaksi Sekarang")
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Auto-sync switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Sync Otomatis", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Upload ke spreadsheet setiap kali transaksi POS selesai", fontSize = 11.sp, color = NeutralMedium)
                            }
                            Switch(
                                checked = autoSync,
                                onCheckedChange = { checked ->
                                    autoSync = checked
                                    coroutineScope.launch {
                                        viewModel.repository.setSetting("auto_sync_sheets", checked.toString())
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Configuration Form Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Konfigurasi API / Webhook:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        // Option A: Apps Script Webhook (Recommended)
                        Text(
                            "Opsi 1: Google Apps Script Webhook URL (Rekomendasi - Tanpa OAuth)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoffeeBrownPrimary
                        )
                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { webhookUrl = it },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("sheets_webhook_input")
                        )

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        // Option B: Google Sheets API v4 Direct
                        Text(
                            "Opsi 2: Google Sheets API v4 Direct",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoffeeBrownPrimary
                        )
                        OutlinedTextField(
                            value = spreadsheetId,
                            onValueChange = { spreadsheetId = it },
                            label = { Text("Spreadsheet ID (dari URL Google Sheet)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("Google Cloud API Key (v4)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        saveStatus?.let { msg ->
                            Text(msg, color = CoffeeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.setSetting("google_sheets_url", webhookUrl.trim())
                                    viewModel.repository.setSetting("google_sheets_id", spreadsheetId.trim())
                                    viewModel.repository.setSetting("google_sheets_api_key", apiKey.trim())
                                    saveStatus = "Konfigurasi Google Sheets berhasil disimpan!"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simpan Pengaturan Sheets")
                        }
                    }
                }
            }

            // Quick Setup Guide
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💡 Cara Setup Google Sheets dalam 2 Menit:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "1. Buka Google Sheets baru di browser laptop.\n" +
                            "2. Klik Extensions > Apps Script.\n" +
                            "3. Tempel fungsi doPost(e) yang menulis baris transaksi.\n" +
                            "4. Klik Deploy > New Deployment > Web App (Who has access: Anyone).\n" +
                            "5. Salin URL Web App dan tempel ke Opsi 1 di atas.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = NeutralDark
                        )
                    }
                }
            }
        }
    }
}
