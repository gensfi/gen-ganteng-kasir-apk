package com.example.ui.screens

import android.widget.Toast
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

                // Action buttons: Bluetooth Print & WhatsApp Digital Receipt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Share
                    OutlinedButton(
                        onClick = {
                            val success = WhatsAppShareHelper.shareToWhatsApp(
                                context = context,
                                phoneNumber = completed.transaction.customerPhone,
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
                        Text("Kirim WA", color = CoffeeBrownDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
