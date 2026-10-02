package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartModalBottomSheet(
    viewModel: CafePosViewModel,
    onDismiss: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()
    val serviceRate by viewModel.serviceRate.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()

    val customerName by viewModel.customerName.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val orderNotes by viewModel.orderNotes.collectAsState()

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    val taxAmount = (subtotal * taxRate) / 100.0
    val serviceAmount = (subtotal * serviceRate) / 100.0
    val totalAmount = subtotal + taxAmount + serviceAmount - discountAmount

    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = CoffeeBrownPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Keranjang Pesanan",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (cartItems.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Kosongkan", color = CoffeeRed)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            errorMessage?.let { err ->
                Surface(
                    color = CoffeeRedLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = err,
                        color = CoffeeRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.RemoveShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = NeutralMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Keranjang masih kosong", color = NeutralMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.menu.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CoffeeBrownDark
                                    )
                                    Text(
                                        currency.format(item.menu.price),
                                        fontSize = 13.sp,
                                        color = CoffeeAmber
                                    )
                                    if (item.notes.isNotBlank()) {
                                        Text(
                                            "Catatan: ${item.notes}",
                                            fontSize = 12.sp,
                                            color = NeutralMedium
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = CoffeeCream,
                                        modifier = Modifier
                                            .size(32.dp)
                                    ) {
                                        IconButton(onClick = {
                                            viewModel.updateCartItemQuantity(item, item.quantity - 1)
                                        }) {
                                            Icon(
                                                Icons.Filled.Remove,
                                                contentDescription = "Kurang",
                                                tint = CoffeeBrownPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${item.quantity}",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        fontSize = 15.sp
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = CoffeeBrownPrimary,
                                        modifier = Modifier
                                            .size(32.dp)
                                    ) {
                                        IconButton(onClick = {
                                            viewModel.updateCartItemQuantity(item, item.quantity + 1)
                                        }) {
                                            Icon(
                                                Icons.Filled.Add,
                                                contentDescription = "Tambah",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(6.dp))
                        // Customer info
                        Text("Informasi Pelanggan (Untuk Struk & WA):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { viewModel.setCustomerName(it) },
                                label = { Text("Nama Pelanggan") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("customer_name_input")
                            )
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { viewModel.setCustomerPhone(it) },
                                label = { Text("WhatsApp (08xx)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("customer_phone_input")
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = orderNotes,
                            onValueChange = { viewModel.setOrderNotes(it) },
                            label = { Text("Nomor Meja / Catatan Pesanan") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("order_notes_input")
                        )
                    }

                    item {
                        Divider(modifier = Modifier.padding(vertical = 10.dp))
                        // Price calculation breakdown
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", color = NeutralMedium)
                            Text(currency.format(subtotal), fontWeight = FontWeight.SemiBold)
                        }
                        if (serviceAmount > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Biaya Layanan (${serviceRate}%)", color = NeutralMedium)
                                Text(currency.format(serviceAmount))
                            }
                        }
                        if (taxAmount > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pajak PB1 (${taxRate}%)", color = NeutralMedium)
                                Text(currency.format(taxAmount))
                            }
                        }
                        if (discountAmount > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Diskon", color = CoffeeRed)
                                Text("-${currency.format(discountAmount)}", color = CoffeeRed)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Bayar", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(currency.format(totalAmount), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CoffeeBrownPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        // Check stock sufficiency
                        viewModel.checkStockSufficiency { isSufficient, insufficientIngredients ->
                            if (isSufficient) {
                                errorMessage = null
                                onProceedToPayment()
                            } else {
                                errorMessage = "Stok bahan tidak mencukupi:\n" + insufficientIngredients.joinToString("\n")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("checkout_button")
                ) {
                    Text(
                        "Lanjut ke Pembayaran (${currency.format(totalAmount)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
