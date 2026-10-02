package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.printer.PrinterStatus
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val printerStatus by viewModel.printerStatus.collectAsState()
    val pairedPrinters by viewModel.pairedPrinters.collectAsState()
    val paperSize by viewModel.paperSize.collectAsState()

    var testPrintMessage by remember { mutableStateOf<String?>(null) }
    var isTestingPrint by remember { mutableStateOf(false) }

    // Bluetooth permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.scanPairedPrinters()
        } else {
            Toast.makeText(context, "Izin Bluetooth diperlukan untuk mencari printer.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
        viewModel.scanPairedPrinters()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Printer Thermal (ESC/POS)", fontWeight = FontWeight.Bold) },
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
            // Connection Status Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status Printer:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (printerStatus) {
                                    is PrinterStatus.Connected -> CoffeeGreenLight
                                    is PrinterStatus.Connecting -> CoffeeOrangeLight
                                    is PrinterStatus.Error -> CoffeeRedLight
                                    else -> NeutralLight
                                }
                            ) {
                                Text(
                                    text = when (val s = printerStatus) {
                                        is PrinterStatus.Connected -> "TERHUBUNG (${s.deviceName})"
                                        is PrinterStatus.Connecting -> "MENGHUBUNGKAN..."
                                        is PrinterStatus.Error -> "ERROR: ${s.message}"
                                        else -> "TERPUTUS"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (printerStatus) {
                                        is PrinterStatus.Connected -> CoffeeGreen
                                        is PrinterStatus.Connecting -> CoffeeOrange
                                        is PrinterStatus.Error -> CoffeeRed
                                        else -> NeutralMedium
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (printerStatus is PrinterStatus.Connected) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { viewModel.disconnectPrinter() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoffeeRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Putuskan Sambungan")
                            }
                        }
                    }
                }
            }

            // Paper Size Setting
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ukuran Kertas Thermal:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Pilih lebar kertas struk printer thermal Anda", fontSize = 12.sp, color = NeutralMedium)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = paperSize == 58,
                                onClick = { viewModel.setPaperSize(58) },
                                label = { Text("58 mm (32 Kolom)") },
                                leadingIcon = {
                                    if (paperSize == 58) Icon(Icons.Filled.Check, contentDescription = null)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = paperSize == 80,
                                onClick = { viewModel.setPaperSize(80) },
                                label = { Text("80 mm (48 Kolom)") },
                                leadingIcon = {
                                    if (paperSize == 80) Icon(Icons.Filled.Check, contentDescription = null)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Test Print Button
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Uji Coba Printer:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Cetak struk uji coba untuk memastikan format ESC/POS terbaca.", fontSize = 12.sp, color = NeutralMedium)
                        Spacer(Modifier.height(10.dp))

                        testPrintMessage?.let { msg ->
                            Surface(
                                color = if (msg.contains("berhasil", ignoreCase = true)) CoffeeGreenLight else CoffeeRedLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = if (msg.contains("berhasil", ignoreCase = true)) CoffeeGreen else CoffeeRed,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isTestingPrint = true
                                testPrintMessage = null
                                viewModel.testPrint { success, msg ->
                                    isTestingPrint = false
                                    testPrintMessage = msg
                                }
                            },
                            enabled = !isTestingPrint,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("test_print_button")
                        ) {
                            if (isTestingPrint) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Test Print Struk")
                            }
                        }
                    }
                }
            }

            // Bluetooth Paired Devices List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Perangkat Bluetooth Terpasang:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = { viewModel.scanPairedPrinters() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Scan Ulang")
                    }
                }
            }

            if (pairedPrinters.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.BluetoothSearching, contentDescription = null, tint = NeutralMedium, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tidak ada printer bluetooth terpasang.",
                                fontSize = 13.sp,
                                color = NeutralMedium
                            )
                            Text(
                                "Pastikan printer sudah di-Pair di Pengaturan Bluetooth HP.",
                                fontSize = 11.sp,
                                color = NeutralMedium
                            )
                        }
                    }
                }
            } else {
                items(pairedPrinters) { device ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Print, contentDescription = null, tint = CoffeeBrownPrimary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(device.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(device.address, fontSize = 11.sp, color = NeutralMedium)
                                }
                            }

                            Button(
                                onClick = { viewModel.connectPrinter(device.address) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Hubungkan", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
