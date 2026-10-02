package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InitialDataSeeder
import com.example.data.local.entity.UserEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    var cafeName by remember { mutableStateOf("") }
    var cafeAddress by remember { mutableStateOf("") }
    var cafePhone by remember { mutableStateOf("") }
    var receiptFooter by remember { mutableStateOf("") }
    var taxPercentage by remember { mutableStateOf("10.0") }
    var servicePercentage by remember { mutableStateOf("5.0") }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    val allUsers by viewModel.repository.getAllUsers().collectAsState(initial = emptyList())
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToChangePassword by remember { mutableStateOf<UserEntity?>(null) }
    var showChangeMyPasswordDialog by remember { mutableStateOf(false) }

    // Backup & Restore state
    var exportedJson by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        cafeName = viewModel.repository.getSetting("cafe_name") ?: "Kopi Kenangan Senja"
        cafeAddress = viewModel.repository.getSetting("cafe_address") ?: "Jl. Malioboro No. 45, Yogyakarta"
        cafePhone = viewModel.repository.getSetting("cafe_phone") ?: "0812-3456-7890"
        receiptFooter = viewModel.repository.getSetting("receipt_footer") ?: "Terima kasih atas kunjungan Anda!\nFollow IG: @kopisenja.cafe"
        taxPercentage = viewModel.repository.getSetting("tax_percentage") ?: "10.0"
        servicePercentage = viewModel.repository.getSetting("service_charge_percentage") ?: "5.0"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Kafe & Sistem", fontWeight = FontWeight.Bold) },
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
            // Profile & Struk Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, tint = CoffeeBrownPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Profil Kafe & Header Struk", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedTextField(
                            value = cafeName,
                            onValueChange = { cafeName = it },
                            label = { Text("Nama Kafe / Usaha") },
                            modifier = Modifier.fillMaxWidth().testTag("setting_cafe_name")
                        )

                        OutlinedTextField(
                            value = cafeAddress,
                            onValueChange = { cafeAddress = it },
                            label = { Text("Alamat Kafe") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = cafePhone,
                            onValueChange = { cafePhone = it },
                            label = { Text("Nomor Telepon / WhatsApp Kafe") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = receiptFooter,
                            onValueChange = { receiptFooter = it },
                            label = { Text("Pesan Penutup Struk (Footer)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        // Tax & Service Charge
                        Text("Pajak & Biaya Layanan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = taxPercentage,
                                onValueChange = { taxPercentage = it },
                                label = { Text("Pajak PB1 (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = servicePercentage,
                                onValueChange = { servicePercentage = it },
                                label = { Text("Service Charge (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        saveStatus?.let {
                            Text(it, color = CoffeeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.setSetting("cafe_name", cafeName.trim())
                                    viewModel.repository.setSetting("cafe_address", cafeAddress.trim())
                                    viewModel.repository.setSetting("cafe_phone", cafePhone.trim())
                                    viewModel.repository.setSetting("receipt_footer", receiptFooter.trim())
                                    viewModel.repository.setSetting("tax_percentage", taxPercentage.trim())
                                    viewModel.repository.setSetting("service_charge_percentage", servicePercentage.trim())
                                    saveStatus = "Pengaturan berhasil diperbarui!"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                        ) {
                            Text("Simpan Pengaturan Kafe")
                        }
                    }
                }
            }

            // User & PIN Authentication Card
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.People, contentDescription = null, tint = BrightOrangePrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Pengguna & Akses PIN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { showChangeMyPasswordDialog = true },
                                    modifier = Modifier.testTag("btn_change_my_pin_settings")
                                ) {
                                    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrightOrangePrimary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ganti PIN Saya", fontSize = 12.sp, color = BrightOrangePrimary, fontWeight = FontWeight.Bold)
                                }
                                if (currentUser?.role == "ADMIN") {
                                    IconButton(
                                        onClick = { showAddUserDialog = true },
                                        modifier = Modifier.testTag("btn_add_user_settings")
                                    ) {
                                        Icon(Icons.Filled.PersonAdd, contentDescription = "Tambah Kasir", tint = BrightAmber)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        allUsers.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (user.role == "ADMIN") BrightOrangePrimary.copy(alpha = 0.15f) else BrightTealLight
                                        ) {
                                            Text(
                                                user.role,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (user.role == "ADMIN") BrightOrangeDark else BrightTeal,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("Username: ${user.username} • PIN: ${user.pin}", fontSize = 11.sp, color = NeutralMedium)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { userToChangePassword = user },
                                        modifier = Modifier.testTag("btn_edit_pin_${user.id}")
                                    ) {
                                        Icon(
                                            Icons.Filled.Key,
                                            contentDescription = "Ganti PIN ${user.name}",
                                            tint = BrightOrangePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (currentUser?.role == "ADMIN" && user.username != "admin") {
                                        IconButton(onClick = {
                                            coroutineScope.launch {
                                                viewModel.repository.deleteUser(user)
                                            }
                                        }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = CoffeeRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Backup & Restore Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Backup, contentDescription = null, tint = CoffeeBrownPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Cadangkan & Pulihkan Database", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Text(
                            "Ekspor seluruh data menu, stok bahan baku, resep, dan pengaturan ke format JSON yang dapat disimpan atau dipulihkan.",
                            fontSize = 12.sp,
                            color = NeutralMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val json = viewModel.backupRestoreManager.exportDatabaseToJson()
                                        exportedJson = json
                                        clipboardManager.setText(AnnotatedString(json))
                                        Toast.makeText(context, "Data cadangan JSON disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ekspor JSON", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showRestoreDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Pulihkan JSON", fontSize = 12.sp)
                            }
                        }

                        exportedJson?.let { json ->
                            Surface(
                                color = NeutralLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)
                            ) {
                                Text(
                                    text = json.take(300) + "...\n[Disalin ke clipboard]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add User Dialog
    if (showAddUserDialog) {
        var newUsername by remember { mutableStateOf("") }
        var newName by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf("KASIR") }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Tambah Akun Kasir / Admin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("PIN Masuk (4-6 Angka)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newRole == "KASIR",
                            onClick = { newRole = "KASIR" },
                            label = { Text("Kasir") }
                        )
                        FilterChip(
                            selected = newRole == "ADMIN",
                            onClick = { newRole = "ADMIN" },
                            label = { Text("Admin / Owner") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank() && newPin.length >= 4) {
                            coroutineScope.launch {
                                viewModel.repository.insertUser(
                                    UserEntity(
                                        username = newUsername.trim(),
                                        name = newName.trim(),
                                        pin = newPin.trim(),
                                        role = newRole
                                    )
                                )
                                showAddUserDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) { Text("Batal") }
            }
        )
    }

    // Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Pulihkan Database dari JSON") },
            text = {
                Column {
                    Text("Tempel teks JSON cadangan ke bawah:", fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("{\"categories\": [...], ...}") },
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.backupRestoreManager.restoreDatabaseFromJson(restoreJsonInput.trim())
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                    showRestoreDialog = false
                                },
                                onFailure = {
                                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                ) {
                    Text("Pulihkan Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Batal") }
            }
        )
    }

    // Change My PIN Dialog (Active Admin)
    if (showChangeMyPasswordDialog) {
        ChangePasswordDialog(
            viewModel = viewModel,
            targetUser = currentUser,
            isAdminReset = false,
            onDismiss = { showChangeMyPasswordDialog = false }
        )
    }

    // Reset User PIN Dialog (Admin changing any user's PIN)
    userToChangePassword?.let { user ->
        ChangePasswordDialog(
            viewModel = viewModel,
            targetUser = user,
            isAdminReset = (user.id != currentUser?.id),
            onDismiss = { userToChangePassword = null }
        )
    }
}
