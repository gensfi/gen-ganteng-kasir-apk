package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSheetsScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var webhookUrl by remember { mutableStateOf("") }
    var spreadsheetId by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var autoSync by remember { mutableStateOf(false) }
    var lastSyncTime by remember { mutableStateOf("-") }
    var saveStatus by remember { mutableStateOf<String?>(null) }

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
