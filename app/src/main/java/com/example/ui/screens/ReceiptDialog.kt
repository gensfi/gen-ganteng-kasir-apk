package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.share.WhatsAppShareHelper
import com.example.ui.CafePosViewModel
import com.example.ui.CompletedTransaction
import com.example.ui.theme.*
import java.io.File

@Composable
fun ReceiptDialog(
    completed: CompletedTransaction,
    viewModel: CafePosViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPrinting by remember { mutableStateOf(false) }
    var printStatusText by remember { mutableStateOf<String?>(null) }
    val paperSize by viewModel.paperSize.collectAsState()
    val cafeLogoPath by viewModel.cafeLogoPath.collectAsState()

    var autoSentWhatsApp by remember { mutableStateOf(false) }
    var recipientPhone by remember { mutableStateOf(completed.transaction.customerPhone ?: "") }
    var showPhoneEdit by remember { mutableStateOf(completed.transaction.customerPhone.isNullOrBlank()) }

    // Otomatis jalankan intent WhatsApp jika nomor pelanggan tersedia
    LaunchedEffect(completed.transaction.id) {
        val initialPhone = completed.transaction.customerPhone
        if (!autoSentWhatsApp && !initialPhone.isNullOrBlank()) {
            autoSentWhatsApp = true
            val ok = WhatsAppShareHelper.shareToWhatsApp(
                context = context,
                phoneNumber = initialPhone,
                receiptText = completed.receiptText
            )
            if (ok) {
                Toast.makeText(context, "Membuka WhatsApp ke $initialPhone...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val logoBitmap = remember(cafeLogoPath) {
        cafeLogoPath?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            } catch (_: Exception) {
                null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CoffeeGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Transaksi Berhasil!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                printStatusText?.let { msg ->
                    Surface(
                        color = if (msg.contains("berhasil", ignoreCase = true)) CoffeeGreenLight else CoffeeRedLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            color = if (msg.contains("berhasil", ignoreCase = true)) CoffeeGreen else CoffeeRed,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Thermal Paper Container (Stylized Monospaced Receipt Preview)
                Surface(
                    color = Color(0xFFFFFDF9),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, NeutralBorder, RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "PRINTER FORMAT (${paperSize}mm ESC/POS)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoffeeAmber
                            )
                            Text("OFFLINE-READY", fontSize = 10.sp, color = CoffeeGreen, fontWeight = FontWeight.Bold)
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp))

                        if (logoBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = logoBitmap.asImageBitmap(),
                                    contentDescription = "Logo Struk",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.height(50.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        Text(
                            text = completed.receiptText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF1E1E1E)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // WhatsApp Delivery Status & Action Section
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (recipientPhone.isNotBlank()) CoffeeGreenLight else PrimaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (recipientPhone.isNotBlank()) CoffeeGreen.copy(alpha = 0.4f) else CoffeeBrownPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = CoffeeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (recipientPhone.isNotBlank()) "Struk WhatsApp Pelanggan" else "Kirim Struk via WhatsApp",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = CoffeeBrownDark
                                )
                            }
                            if (recipientPhone.isNotBlank()) {
                                TextButton(
                                    onClick = { showPhoneEdit = !showPhoneEdit },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(if (showPhoneEdit) "Tutup" else "Ubah No. WA", fontSize = 11.sp, color = CoffeeBrownPrimary)
                                }
                            }
                        }

                        if (recipientPhone.isNotBlank() && !showPhoneEdit) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Tujuan: $recipientPhone",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeutralDark
                                )
                                Button(
                                    onClick = {
                                        val success = WhatsAppShareHelper.shareToWhatsApp(
                                            context = context,
                                            phoneNumber = recipientPhone,
                                            receiptText = completed.receiptText
                                        )
                                        if (success) {
                                            Toast.makeText(context, "Membuka WhatsApp ke $recipientPhone...", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Kirim Ulang WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Phone input field for custom / new number
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = recipientPhone,
                                    onValueChange = { recipientPhone = it.filter { ch -> ch.isDigit() || ch == '+' } },
                                    placeholder = { Text("Contoh: 081234567890", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                )
                                Button(
                                    onClick = {
                                        if (recipientPhone.isBlank()) {
                                            Toast.makeText(context, "Masukkan nomor WhatsApp pelanggan!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val success = WhatsAppShareHelper.shareToWhatsApp(
                                                context = context,
                                                phoneNumber = recipientPhone,
                                                receiptText = completed.receiptText
                                            )
                                            if (success) {
                                                Toast.makeText(context, "Membuka WhatsApp ke $recipientPhone...", Toast.LENGTH_SHORT).show()
                                                showPhoneEdit = false
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeGreen),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Kirim WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Action buttons: Bluetooth Print & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Generic WhatsApp / Share
                    OutlinedButton(
                        onClick = {
                            val success = WhatsAppShareHelper.shareToWhatsApp(
                                context = context,
                                phoneNumber = recipientPhone.ifBlank { completed.transaction.customerPhone },
                                receiptText = completed.receiptText
                            )
                            if (!success) {
                                Toast.makeText(context, "Tidak dapat membuka WhatsApp.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("whatsapp_receipt_button")
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = null,
                            tint = CoffeeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Bagikan", color = CoffeeBrownDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Bluetooth Print
                    Button(
                        onClick = {
                            isPrinting = true
                            printStatusText = "Mengirim ke printer bluetooth..."
                            viewModel.printCurrentReceipt(completed) { success, msg ->
                                isPrinting = false
                                printStatusText = msg
                            }
                        },
                        enabled = !isPrinting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_receipt_button")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPrinting) "Mencetak..." else "Cetak Struk", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // New Transaction button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mulai Transaksi Baru", fontWeight = FontWeight.Bold, color = CoffeeAmber)
                }
            }
        }
    }
}
