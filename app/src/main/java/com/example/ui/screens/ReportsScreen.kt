package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ReportPeriodMode {
    TODAY,
    YESTERDAY,
    SPECIFIC_DAY,
    CUSTOM_RANGE,
    LAST_7_DAYS,
    LAST_30_DAYS,
    ALL_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val reportSummary by viewModel.reportSummary.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()

    var periodMode by remember { mutableStateOf(ReportPeriodMode.TODAY) }

    // Selected specific single day (defaults to today)
    var specificDayCalendar by remember {
        mutableStateOf(Calendar.getInstance())
    }

    // Selected custom range
    var rangeStartCalendar by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -6)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        )
    }

    var rangeEndCalendar by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        )
    }

    // Modal date picker dialog state
    var showSingleDayPicker by remember { mutableStateOf(false) }
    var showRangeStartPicker by remember { mutableStateOf(false) }
    var showRangeEndPicker by remember { mutableStateOf(false) }

    // Search and filter for transaction history
    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentFilter by remember { mutableStateOf("ALL") }

    var selectedTransactionDetails by remember { mutableStateOf<Pair<TransactionEntity, List<TransactionItemEntity>>?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }
    val dateTimeFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val fullDayFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")) }

    // Step day helper for specific day navigation
    fun stepSpecificDay(delta: Int) {
        val newCal = (specificDayCalendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, delta)
        }
        specificDayCalendar = newCal
        periodMode = ReportPeriodMode.SPECIFIC_DAY
    }

    // Presets for Custom Range
    fun setPresetThisWeek() {
        val start = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        rangeStartCalendar = start
        rangeEndCalendar = end
        periodMode = ReportPeriodMode.CUSTOM_RANGE
    }

    fun setPresetThisMonth() {
        val start = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        rangeStartCalendar = start
        rangeEndCalendar = end
        periodMode = ReportPeriodMode.CUSTOM_RANGE
    }

    fun setPresetLastMonth() {
        val start = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        rangeStartCalendar = start
        rangeEndCalendar = end
        periodMode = ReportPeriodMode.CUSTOM_RANGE
    }

    // Calculate timestamps
    val (startTimestamp, endTimestamp, periodLabel) = remember(
        periodMode,
        specificDayCalendar.timeInMillis,
        rangeStartCalendar.timeInMillis,
        rangeEndCalendar.timeInMillis
    ) {
        val now = Calendar.getInstance()
        when (periodMode) {
            ReportPeriodMode.TODAY -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                Triple(start, end, "Hari Ini (${displayDateFormat.format(Date(now.timeInMillis))})")
            }
            ReportPeriodMode.YESTERDAY -> {
                val yCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val start = (yCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = (yCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                Triple(start, end, "Kemarin (${displayDateFormat.format(Date(start))})")
            }
            ReportPeriodMode.SPECIFIC_DAY -> {
                val start = (specificDayCalendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = (specificDayCalendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                Triple(start, end, fullDayFormat.format(Date(start)))
            }
            ReportPeriodMode.CUSTOM_RANGE -> {
                val minCal = if (rangeStartCalendar.timeInMillis <= rangeEndCalendar.timeInMillis) rangeStartCalendar else rangeEndCalendar
                val maxCal = if (rangeStartCalendar.timeInMillis > rangeEndCalendar.timeInMillis) rangeStartCalendar else rangeEndCalendar
                val start = (minCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = (maxCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                Triple(start, end, "${displayDateFormat.format(Date(start))} - ${displayDateFormat.format(Date(end))}")
            }
            ReportPeriodMode.LAST_7_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                Triple(start, end, "7 Hari Terakhir (${displayDateFormat.format(Date(start))} - ${displayDateFormat.format(Date(end))})")
            }
            ReportPeriodMode.LAST_30_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                Triple(start, end, "30 Hari Terakhir (${displayDateFormat.format(Date(start))} - ${displayDateFormat.format(Date(end))})")
            }
            ReportPeriodMode.ALL_TIME -> {
                Triple(0L, Long.MAX_VALUE, "Semua Riwayat Penjualan")
            }
        }
    }

    // Filter transactions to matching period
    val filteredTransactions = remember(allTransactions, startTimestamp, endTimestamp) {
        if (periodMode == ReportPeriodMode.ALL_TIME) {
            allTransactions
        } else {
            allTransactions.filter { it.timestamp in startTimestamp..endTimestamp }
        }
    }

    // Further filter for display based on search query & payment method
    val displayedTransactions = remember(filteredTransactions, searchQuery, selectedPaymentFilter) {
        filteredTransactions.filter { trx ->
            val matchesSearch = searchQuery.isBlank() ||
                    trx.id.contains(searchQuery, ignoreCase = true) ||
                    (trx.customerName ?: "").contains(searchQuery, ignoreCase = true) ||
                    trx.cashierName.contains(searchQuery, ignoreCase = true)
            val matchesPayment = when (selectedPaymentFilter) {
                "CASH" -> trx.paymentMethod.equals("CASH", ignoreCase = true)
                "QRIS" -> trx.paymentMethod.equals("QRIS", ignoreCase = true)
                "ONLINE" -> trx.paymentMethod.equals("ONLINE", ignoreCase = true) ||
                        trx.paymentMethod.equals("PESANAN_ONLINE", ignoreCase = true) ||
                        trx.paymentMethod.equals("TRANSFER", ignoreCase = true)
                "TRANSFER" -> trx.paymentMethod.equals("ONLINE", ignoreCase = true) ||
                        trx.paymentMethod.equals("PESANAN_ONLINE", ignoreCase = true) ||
                        trx.paymentMethod.equals("TRANSFER", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesPayment
        }
    }

    // Trigger report calculation whenever time bounds change
    LaunchedEffect(startTimestamp, endTimestamp, periodMode) {
        if (periodMode == ReportPeriodMode.ALL_TIME) {
            viewModel.loadReportsForRange(0L, Long.MAX_VALUE)
        } else {
            viewModel.loadReportsForRange(startTimestamp, endTimestamp)
        }
    }

    // Material 3 Date Picker Dialog for Specific Single Day
    if (showSingleDayPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = specificDayCalendar.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showSingleDayPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            specificDayCalendar = newCal
                            periodMode = ReportPeriodMode.SPECIFIC_DAY
                        }
                        showSingleDayPicker = false
                    }
                ) {
                    Text("Pilih Hari Ini", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSingleDayPicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Date Picker Dialog for Range Start Date (Langkah 1/2)
    if (showRangeStartPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = rangeStartCalendar.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRangeStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            rangeStartCalendar = newCal
                            // If current end date is before the new start date, update end date to end of start date's day
                            if (rangeEndCalendar.before(newCal)) {
                                rangeEndCalendar = (newCal.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }
                            }
                            periodMode = ReportPeriodMode.CUSTOM_RANGE
                        }
                        showRangeStartPicker = false
                        // Automatically open Step 2: Tanggal Akhir Picker
                        showRangeEndPicker = true
                    }
                ) {
                    Text("Lanjut Pilih Tanggal Akhir ➜", fontWeight = FontWeight.Bold, color = CoffeeBrownPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRangeStartPicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            Column {
                Surface(
                    color = PrimaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🗓️ Langkah 1 dari 2: Pilih Tanggal Mulai (Awal)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = CoffeeBrownDark,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                DatePicker(state = datePickerState)
            }
        }
    }

    // Material 3 Date Picker Dialog for Range End Date (Langkah 2/2)
    if (showRangeEndPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = rangeEndCalendar.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRangeEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }
                            rangeEndCalendar = newCal
                            // If user picked end date before start date, ensure start date matches start of that day
                            if (newCal.before(rangeStartCalendar)) {
                                rangeStartCalendar = (newCal.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                            }
                            periodMode = ReportPeriodMode.CUSTOM_RANGE
                        }
                        showRangeEndPicker = false
                    }
                ) {
                    Text("Terapkan Rentang Waktu ✓", fontWeight = FontWeight.Bold, color = CoffeeGreen)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showRangeEndPicker = false
                        showRangeStartPicker = true
                    }) {
                        Text("Kembali ke Awal")
                    }
                    TextButton(onClick = { showRangeEndPicker = false }) {
                        Text("Batal")
                    }
                }
            }
        ) {
            Column {
                Surface(
                    color = CoffeeGreenLight,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🗓️ Langkah 2 dari 2: Pilih Tanggal Selesai (Akhir)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = CoffeeGreen,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                DatePicker(state = datePickerState)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Laporan & Riwayat Penjualan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = periodLabel,
                            fontSize = 11.5.sp,
                            color = NeutralMedium,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isGeneratingPdf = true
                                try {
                                    val cafeName = viewModel.repository.getSetting("cafe_name") ?: "Rencana Kopi"
                                    val cafeAddress = viewModel.repository.getSetting("cafe_address") ?: "Jl. Kopi No. 1"
                                    val cafePhone = viewModel.repository.getSetting("cafe_phone") ?: "0812-3456-7890"

                                    val allItems = withContext(Dispatchers.IO) {
                                        viewModel.repository.getAllTransactionItems()
                                    }
                                    val trxIds = filteredTransactions.map { it.id }.toSet()
                                    val periodItems = allItems.filter { it.transactionId in trxIds }

                                    val cashTotal = filteredTransactions.filter { it.paymentMethod == "CASH" }.sumOf { it.totalAmount }
                                    val qrisTotal = filteredTransactions.filter { it.paymentMethod == "QRIS" }.sumOf { it.totalAmount }
                                    val onlineTotal = filteredTransactions.filter { it.paymentMethod.equals("ONLINE", true) || it.paymentMethod.equals("PESANAN_ONLINE", true) || it.paymentMethod.equals("TRANSFER", true) }.sumOf { it.totalAmount }

                                    val pdfData = PdfReportGenerator.PdfReportData(
                                        cafeName = cafeName,
                                        cafeAddress = cafeAddress,
                                        cafePhone = cafePhone,
                                        startDateFormatted = if (periodMode == ReportPeriodMode.ALL_TIME) "Awal Toko" else displayDateFormat.format(Date(startTimestamp)),
                                        endDateFormatted = if (periodMode == ReportPeriodMode.ALL_TIME) "Sekarang" else displayDateFormat.format(Date(endTimestamp)),
                                        transactions = filteredTransactions,
                                        items = periodItems,
                                        totalRevenue = reportSummary?.totalRevenue ?: filteredTransactions.sumOf { it.totalAmount },
                                        totalNetProfit = reportSummary?.totalNetProfit ?: 0.0,
                                        cashTotal = cashTotal,
                                        qrisTotal = qrisTotal,
                                        onlineTotal = onlineTotal,
                                        transferTotal = onlineTotal
                                    )

                                    val file = PdfReportGenerator.generateSalesPdf(context, pdfData)
                                    if (file != null) {
                                        PdfReportGenerator.openPdf(context, file)
                                    } else {
                                        Toast.makeText(context, "Gagal membuat file PDF laporan", Toast.LENGTH_SHORT).show()
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
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Ekspor PDF", tint = BrightRed)
                        }
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
            // Main Period Selection Control Box
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Pilih Periode Laporan:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoffeeBrownDark
                            )
                            Surface(
                                color = PrimaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${filteredTransactions.size} Transaksi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoffeeBrownPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Prominent Buttons for: [PILIH HARI 📅] and [RENTANG WAKTU 🗓️]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    periodMode = ReportPeriodMode.SPECIFIC_DAY
                                    showSingleDayPicker = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (periodMode == ReportPeriodMode.SPECIFIC_DAY) CoffeeBrownPrimary else PrimaryContainer,
                                    contentColor = if (periodMode == ReportPeriodMode.SPECIFIC_DAY) Color.White else CoffeeBrownDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_select_specific_day")
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pilih Hari 📅", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }

                            Button(
                                onClick = {
                                    periodMode = ReportPeriodMode.CUSTOM_RANGE
                                    showRangeStartPicker = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (periodMode == ReportPeriodMode.CUSTOM_RANGE) CoffeeBrownPrimary else PrimaryContainer,
                                    contentColor = if (periodMode == ReportPeriodMode.CUSTOM_RANGE) Color.White else CoffeeBrownDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_select_custom_range")
                            ) {
                                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Rentang Waktu 🗓️", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }

                        // Preset Horizontal Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = periodMode == ReportPeriodMode.TODAY,
                                onClick = { periodMode = ReportPeriodMode.TODAY },
                                leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                label = { Text("Hari Ini", fontSize = 11.5.sp) }
                            )

                            FilterChip(
                                selected = periodMode == ReportPeriodMode.YESTERDAY,
                                onClick = { periodMode = ReportPeriodMode.YESTERDAY },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                label = { Text("Kemarin", fontSize = 11.5.sp) }
                            )

                            FilterChip(
                                selected = periodMode == ReportPeriodMode.LAST_7_DAYS,
                                onClick = { periodMode = ReportPeriodMode.LAST_7_DAYS },
                                label = { Text("7 Hari Terakhir", fontSize = 11.5.sp) }
                            )

                            FilterChip(
                                selected = periodMode == ReportPeriodMode.LAST_30_DAYS,
                                onClick = { periodMode = ReportPeriodMode.LAST_30_DAYS },
                                label = { Text("30 Hari Terakhir", fontSize = 11.5.sp) }
                            )

                            FilterChip(
                                selected = periodMode == ReportPeriodMode.ALL_TIME,
                                onClick = { periodMode = ReportPeriodMode.ALL_TIME },
                                label = { Text("Semua Riwayat", fontSize = 11.5.sp) }
                            )
                        }
                    }
                }
            }

            // Interactive Details Banner for "PILIH HARI"
            if (periodMode == ReportPeriodMode.SPECIFIC_DAY) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryContainer.copy(alpha = 0.55f)),
                        border = BorderStroke(1.dp, CoffeeBrownPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CalendarToday,
                                        contentDescription = null,
                                        tint = CoffeeBrownPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Laporan Penjualan Hari Tertentu", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeutralMedium)
                                        Text(
                                            fullDayFormat.format(Date(startTimestamp)),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = CoffeeBrownDark
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { showSingleDayPicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.EditCalendar, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ganti Hari", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quick Day Navigation Row (Previous Day, Today, Next Day)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { stepSpecificDay(-1) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("H-1 Kemarin", fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        specificDayCalendar = Calendar.getInstance()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Hari Ini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { stepSpecificDay(1) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("H+1 Esok", fontSize = 11.sp, maxLines = 1)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Details Banner for "RENTANG WAKTU"
            if (periodMode == ReportPeriodMode.CUSTOM_RANGE) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryContainer.copy(alpha = 0.55f)),
                        border = BorderStroke(1.dp, CoffeeBrownPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = CoffeeBrownPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Laporan Berdasarkan Rentang Waktu", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeutralMedium)
                                    Text(
                                        "${displayDateFormat.format(Date(startTimestamp))} — ${displayDateFormat.format(Date(endTimestamp))}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = CoffeeBrownDark
                                    )
                                }
                            }

                            // Two prominent buttons: [Dari Tanggal] and [Sampai Tanggal]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.5.dp, CoffeeBrownPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showRangeStartPicker = true }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("📅 Tanggal Mulai (Awal):", fontSize = 10.sp, color = NeutralMedium, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(2.dp))
                                        Text(displayDateFormat.format(Date(startTimestamp)), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = CoffeeBrownDark)
                                        Text("Ubah Tanggal Awal ✎", fontSize = 9.5.sp, color = CoffeeBrownPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.5.dp, CoffeeGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showRangeEndPicker = true }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("📅 Tanggal Akhir (Selesai):", fontSize = 10.sp, color = NeutralMedium, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(2.dp))
                                        Text(displayDateFormat.format(Date(endTimestamp)), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = CoffeeBrownDark)
                                        Text("Ubah Tanggal Akhir ✎", fontSize = 9.5.sp, color = CoffeeGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Full-width button to reconfigure both dates in sequence
                            Button(
                                onClick = { showRangeStartPicker = true },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                            ) {
                                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Atur Rentang Tanggal (Mulai ➔ Selesai)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Quick Range Presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = { setPresetThisWeek() },
                                    label = { Text("Minggu Ini", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = { setPresetThisMonth() },
                                    label = { Text("Bulan Ini", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = { setPresetLastMonth() },
                                    label = { Text("Bulan Lalu", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Active Period Header Status Card
            item {
                Surface(
                    color = PrimaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = CoffeeBrownPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Periode Aktif: $periodLabel",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoffeeBrownDark
                            )
                        }

                        Text(
                            "${filteredTransactions.size} Transaksi",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoffeeBrownPrimary
                        )
                    }
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
                                value = "${filteredTransactions.size} Pesanan",
                                color = CoffeeAmber,
                                icon = Icons.Filled.ReceiptLong,
                                modifier = Modifier.weight(1f)
                            )
                            ReportCard(
                                title = "Modal / HPP",
                                value = currency.format((rep.totalRevenue - rep.totalNetProfit).coerceAtLeast(0.0)),
                                color = NeutralMedium,
                                icon = Icons.Filled.Inventory,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Payment Methods breakdown in period
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Rincian Metode Pembayaran Periode Ini", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                val cashSum = filteredTransactions.filter { it.paymentMethod == "CASH" }.sumOf { it.totalAmount }
                                val qrisSum = filteredTransactions.filter { it.paymentMethod == "QRIS" }.sumOf { it.totalAmount }
                                val onlineSum = filteredTransactions.filter { it.paymentMethod.equals("ONLINE", true) || it.paymentMethod.equals("PESANAN_ONLINE", true) || it.paymentMethod.equals("TRANSFER", true) }.sumOf { it.totalAmount }

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("💵 Tunai (Cash):", fontSize = 12.sp, color = NeutralMedium)
                                    Text(currency.format(cashSum), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = CoffeeBrownDark)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("📱 QRIS:", fontSize = 12.sp, color = NeutralMedium)
                                    Text(currency.format(qrisSum), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = CoffeeGreen)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("🛍️ Pesanan Online:", fontSize = 12.sp, color = NeutralMedium)
                                    Text(currency.format(onlineSum), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = BrightOrangePrimary)
                                }
                            }
                        }

                        // Top Selling Items
                        if (rep.topSellingItems.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "⭐ Menu Terlaris Periode Ini",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CoffeeBrownDark
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    rep.topSellingItems.take(5).forEachIndexed { idx, pair ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${idx + 1}. ${pair.first}", fontSize = 12.5.sp)
                                            Text("${pair.second} terjual", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = CoffeeBrownPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        // Ingredients Usage
                        if (rep.ingredientUsageSummary.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "📦 Penggunaan Bahan Baku Periode Ini",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CoffeeBrownDark
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    rep.ingredientUsageSummary.forEach { pair ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(pair.first, fontSize = 12.5.sp)
                                            Text(pair.second, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = CoffeeBrownPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Transaction History Section Header with Search & Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Riwayat Penjualan (${displayedTransactions.size}/${filteredTransactions.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CoffeeBrownDark
                        )

                        if (displayedTransactions.isNotEmpty()) {
                            Text(
                                "Total: ${currency.format(displayedTransactions.sumOf { it.totalAmount })}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoffeeBrownPrimary
                            )
                        }
                    }

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari struk / kasir / pelanggan...", fontSize = 12.5.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Hapus pencarian", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )

                    // Payment Method Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedPaymentFilter == "ALL",
                            onClick = { selectedPaymentFilter = "ALL" },
                            label = { Text("Semua Metode", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedPaymentFilter == "CASH",
                            onClick = { selectedPaymentFilter = "CASH" },
                            label = { Text("💵 Tunai (Cash)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedPaymentFilter == "QRIS",
                            onClick = { selectedPaymentFilter = "QRIS" },
                            label = { Text("📱 QRIS", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedPaymentFilter == "ONLINE" || selectedPaymentFilter == "TRANSFER",
                            onClick = { selectedPaymentFilter = "ONLINE" },
                            label = { Text("🛍️ Pesanan Online", fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (displayedTransactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeutralLight,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.EventBusy, contentDescription = null, tint = NeutralMedium, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Tidak ada riwayat transaksi pada periode/filter ini.", color = NeutralMedium, fontSize = 13.sp)
                            Text("Coba pilih hari lain, rentang waktu lain, atau reset pencarian.", color = NeutralMedium, fontSize = 11.5.sp)
                        }
                    }
                }
            } else {
                items(displayedTransactions, key = { it.id }) { trx ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trx.id, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text(dateTimeFormat.format(Date(trx.timestamp)), fontSize = 11.sp, color = NeutralMedium)
                                Text(
                                    "Kasir: ${trx.cashierName} • ${trx.paymentMethod}${if (!trx.customerName.isNullOrBlank()) " • ${trx.customerName}" else ""}",
                                    fontSize = 11.sp,
                                    color = NeutralMedium
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    currency.format(trx.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
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
                    Text("Waktu: ${dateTimeFormat.format(Date(trx.timestamp))}", fontSize = 12.sp)
                    Text("Kasir: ${trx.cashierName}", fontSize = 12.sp)
                    if (!trx.customerName.isNullOrBlank()) {
                        Text("Pelanggan: ${trx.customerName}", fontSize = 12.sp)
                    }
                    Text("Metode Bayar: ${trx.paymentMethod}", fontSize = 12.sp)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    items.forEach { itm ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${itm.quantity}x ${itm.menuName}", fontSize = 13.sp)
                            Text(currency.format(itm.price * itm.quantity), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
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
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
