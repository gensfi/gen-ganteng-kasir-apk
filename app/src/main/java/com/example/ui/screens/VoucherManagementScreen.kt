package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.VoucherEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherManagementScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vouchers by viewModel.vouchers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Manajemen Voucher & Promo", fontWeight = FontWeight.Bold)
                        Text("Pengaturan diskon kode promo kafe", fontSize = 11.5.sp, color = NeutralMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("btn_add_voucher_top")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Tambah Voucher", tint = BrightOrangePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrightOrangePrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Buat Voucher", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_create_voucher")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrightPeach.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, tint = BrightOrangePrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Kelola Kupon Promo Kafe", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrightOrangeDark)
                            Text("Voucher yang aktif dapat dimasukkan kasir di layar keranjang belanja.", fontSize = 11.5.sp, color = NeutralMedium)
                        }
                    }
                }
            }

            if (vouchers.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralLight),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, tint = NeutralMedium, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Belum ada kode voucher", fontWeight = FontWeight.Bold, color = NeutralMedium)
                            Text("Tekan tombol 'Buat Voucher' untuk membuat kode promo baru.", fontSize = 12.sp, color = NeutralMedium)
                        }
                    }
                }
            }

            items(vouchers, key = { it.code }) { voucher ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrightOrangePrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = voucher.code,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = BrightOrangeDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                if (voucher.isExpired()) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = BrightRedLight) {
                                        Text("Kedaluwarsa", fontSize = 10.sp, color = BrightRed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                } else if (voucher.isQuotaExceeded()) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = NeutralLight) {
                                        Text("Kuota Habis", fontSize = 10.sp, color = NeutralMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                } else if (voucher.isActive) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = FreshGreenLight) {
                                        Text("Aktif", fontSize = 10.sp, color = FreshGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                } else {
                                    Surface(shape = RoundedCornerShape(6.dp), color = NeutralLight) {
                                        Text("Nonaktif", fontSize = 10.sp, color = NeutralMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = voucher.isActive,
                                    onCheckedChange = { viewModel.toggleVoucherStatus(voucher) }
                                )
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        viewModel.deleteVoucher(voucher.code) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = BrightRed)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = NeutralBorder)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Besaran Diskon", fontSize = 11.sp, color = NeutralMedium)
                                Text(
                                    if (voucher.discountType == "PERCENT") "${voucher.discountValue.toInt()}% Potongan" else "${currency.format(voucher.discountValue)} Potongan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = NeutralDark
                                )
                            }
                            Column {
                                Text("Min. Pembelian", fontSize = 11.sp, color = NeutralMedium)
                                Text(
                                    if (voucher.minPurchase > 0) currency.format(voucher.minPurchase) else "Tanpa Min.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = NeutralDark
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Kuota Digunakan", fontSize = 11.sp, color = NeutralMedium)
                                Text(
                                    if (voucher.quota > 0) "${voucher.usedCount} / ${voucher.quota}" else "${voucher.usedCount} / ∞",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = NeutralDark
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Berlaku s/d: ${dateFormat.format(Date(voucher.expiryDate))}",
                            fontSize = 11.sp,
                            color = NeutralMedium
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(60.dp))
            }
        }
    }

    // Add / Edit Voucher Dialog
    if (showAddDialog) {
        var code by remember { mutableStateOf("") }
        var discountType by remember { mutableStateOf("PERCENT") }
        var discountValueStr by remember { mutableStateOf("20") }
        var minPurchaseStr by remember { mutableStateOf("25000") }
        var quotaStr by remember { mutableStateOf("100") }
        var expiryDateMillis by remember { mutableStateOf(System.currentTimeMillis() + (90L * 24 * 3600 * 1000)) }

        val cal = Calendar.getInstance().apply { timeInMillis = expiryDateMillis }
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                }
                expiryDateMillis = selected.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, tint = BrightOrangePrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Buat Voucher Promo Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Kode Voucher (Contoh: PROMO20)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_new_voucher_code")
                    )

                    Text("Jenis Potongan:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = discountType == "PERCENT",
                            onClick = {
                                discountType = "PERCENT"
                                if (discountValueStr == "10000") discountValueStr = "20"
                            },
                            label = { Text("Persentase (%)") }
                        )
                        FilterChip(
                            selected = discountType == "NOMINAL",
                            onClick = {
                                discountType = "NOMINAL"
                                if (discountValueStr == "20") discountValueStr = "10000"
                            },
                            label = { Text("Nominal (Rp)") }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountValueStr,
                            onValueChange = { discountValueStr = it },
                            label = { Text(if (discountType == "PERCENT") "Nilai Diskon (%)" else "Nilai Diskon (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("input_new_voucher_value")
                        )
                        OutlinedTextField(
                            value = minPurchaseStr,
                            onValueChange = { minPurchaseStr = it },
                            label = { Text("Min. Belanja (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("input_new_voucher_min")
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quotaStr,
                            onValueChange = { quotaStr = it },
                            label = { Text("Kuota Pakai (-1: Tanpa Batas)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("input_new_voucher_quota")
                        )
                    }

                    // Expiry Date Selection
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeutralLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Masa Berlaku Voucher", fontSize = 10.5.sp, color = NeutralMedium)
                                Text(dateFormat.format(Date(expiryDateMillis)), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = BrightOrangePrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanCode = code.trim().uppercase()
                        val valNum = discountValueStr.toDoubleOrNull() ?: 0.0
                        val minNum = minPurchaseStr.toDoubleOrNull() ?: 0.0
                        val quotaNum = quotaStr.toIntOrNull() ?: 100

                        if (cleanCode.isBlank()) {
                            Toast.makeText(context, "Kode voucher wajib diisi!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (valNum <= 0) {
                            Toast.makeText(context, "Nilai diskon harus lebih besar dari 0!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val newVoucher = VoucherEntity(
                            code = cleanCode,
                            discountType = discountType,
                            discountValue = valNum,
                            minPurchase = minNum,
                            quota = quotaNum,
                            usedCount = 0,
                            expiryDate = expiryDateMillis,
                            isActive = true
                        )

                        viewModel.createOrUpdateVoucher(newVoucher) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightOrangePrimary),
                    modifier = Modifier.testTag("btn_save_voucher_submit")
                ) {
                    Text("Simpan Voucher", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
