package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import com.example.util.SoundHelper
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentDialog(
    viewModel: CafePosViewModel,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val context = LocalContext.current
    val subtotal by viewModel.cartSubtotal.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()
    val serviceRate by viewModel.serviceRate.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()

    val taxAmount = (subtotal * taxRate) / 100.0
    val serviceAmount = (subtotal * serviceRate) / 100.0
    val totalAmount = subtotal + taxAmount + serviceAmount - discountAmount

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    var selectedTab by remember { mutableStateOf("CASH") } // CASH, QRIS, TRANSFER
    var cashGivenText by remember { mutableStateOf(totalAmount.toLong().toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val cashGiven = cashGivenText.toDoubleOrNull() ?: 0.0
    val changeAmount = cashGiven - totalAmount

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Pembayaran",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, enabled = !isProcessing) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                // Total Amount Banner
                Surface(
                    color = PrimaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL TAGIHAN", fontSize = 12.sp, color = NeutralMedium, fontWeight = FontWeight.Bold)
                        Text(
                            currency.format(totalAmount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoffeeBrownPrimary
                        )
                    }
                }

                // Payment Method Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodTab(
                        title = "Tunai",
                        icon = Icons.Filled.Payments,
                        isSelected = selectedTab == "CASH",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "CASH" }
                    )
                    PaymentMethodTab(
                        title = "QRIS",
                        icon = Icons.Filled.QrCodeScanner,
                        isSelected = selectedTab == "QRIS",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "QRIS" }
                    )
                    PaymentMethodTab(
                        title = "Transfer",
                        icon = Icons.Filled.AccountBalance,
                        isSelected = selectedTab == "TRANSFER",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "TRANSFER" }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Content based on method
                when (selectedTab) {
                    "CASH" -> {
                        Text("Jumlah Tunai Diterima:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = cashGivenText,
                            onValueChange = { input ->
                                cashGivenText = input.filter { it.isDigit() }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            prefix = { Text("Rp ") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cash_input_field")
                        )

                        // Quick cash nominal chips
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickCashButton("Pas", totalAmount.toLong()) { cashGivenText = it.toString() }
                            QuickCashButton("50k", 50000) { cashGivenText = it.toString() }
                            QuickCashButton("100k", 100000) { cashGivenText = it.toString() }
                            QuickCashButton("200k", 200000) { cashGivenText = it.toString() }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Kembalian Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (changeAmount >= 0) CoffeeGreenLight else CoffeeRedLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (changeAmount >= 0) "Kembalian" else "Kurang Bayar",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (changeAmount >= 0) CoffeeGreen else CoffeeRed
                                )
                                Text(
                                    currency.format(kotlin.math.abs(changeAmount)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (changeAmount >= 0) CoffeeGreen else CoffeeRed
                                )
                            }
                        }
                    }

                    "QRIS" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(150.dp)
                                    .border(1.dp, NeutralBorder, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.QrCode2,
                                        contentDescription = "QRIS Code",
                                        tint = NeutralDark,
                                        modifier = Modifier.size(100.dp)
                                    )
                                    Text("QRIS DINAMIS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoffeeAmber)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("KOPI KENANGAN SENJA", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("NMID: ID1029384756 | A.N. CAFE POS", fontSize = 11.sp, color = NeutralMedium)
                        }
                    }

                    "TRANSFER" -> {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("Transfer Bank Manual:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NeutralLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("BCA: 873-019-2810 (Kopi Senja)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Mandiri: 137-00-1928-111 (Kopi Senja)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Pastikan nominal transfer sesuai dengan total tagihan.", fontSize = 11.sp, color = NeutralMedium)
                                }
                            }
                        }
                    }
                }

                errorMessage?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = CoffeeRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (selectedTab == "CASH" && changeAmount < 0) {
                            errorMessage = "Nominal uang tunai kurang dari total belanja!"
                            return@Button
                        }
                        isProcessing = true
                        errorMessage = null
                        SoundHelper.playCashChime(context)

                        viewModel.processPayment(
                            paymentMethod = selectedTab,
                            cashGiven = if (selectedTab == "CASH") cashGiven else totalAmount,
                            onSuccess = {
                                isProcessing = false
                                onPaymentSuccess()
                            },
                            onError = { err ->
                                isProcessing = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_payment_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text("Konfirmasi & Bayar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.QuickCashButton(label: String, value: Long, onClick: (Long) -> Unit) {
    OutlinedButton(
        onClick = { onClick(value) },
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = Modifier.weight(1f).height(36.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoffeeBrownDark)
    }
}

@Composable
fun PaymentMethodTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) CoffeeBrownPrimary else NeutralLight,
        modifier = modifier
            .height(52.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else NeutralMedium,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else NeutralMedium
            )
        }
    }
}
