package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.BackupFileInfo
import com.example.data.local.InitialDataSeeder
import com.example.data.local.entity.UserEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var taxSaveStatus by remember { mutableStateOf<String?>(null) }

    val cafeLogoPath by viewModel.cafeLogoPath.collectAsState()
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val logoFile = File(context.filesDir, "cafe_logo.png")
                        val outputStream = FileOutputStream(logoFile)
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        outputStream.close()
                        viewModel.updateCafeLogoPath(logoFile.absolutePath)
                        Toast.makeText(context, "Logo kafe berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal mengunggah logo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val allUsers by viewModel.repository.getAllUsers().collectAsState(initial = emptyList())
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var userToChangePassword by remember { mutableStateOf<UserEntity?>(null) }
    var showChangeMyPasswordDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }

    // Backup & Restore state
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()
    val lastAutoBackupTime by viewModel.lastAutoBackupTime.collectAsState()
    val backupFiles by viewModel.backupFiles.collectAsState()
    var fileToRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var backupStatusMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val backupDateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }
    var exportedJson by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }

    // Kembali ke Pengaturan Awal (Factory Reset) states
    var showResetToFactoryDialog by remember { mutableStateOf(false) }
    var resetAdminPinInput by remember { mutableStateOf("") }
    var resetPinError by remember { mutableStateOf<String?>(null) }
    var resetIncludeSampleData by remember { mutableStateOf(true) }
    var isResetting by remember { mutableStateOf(false) }

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

                        // Logo Upload Section
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeutralLight.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Logo Kafe & Struk Pembayaran",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CoffeeBrownDark
                                )
                                Text(
                                    "Logo ini akan dicetak di bagian paling atas struk thermal & struk WhatsApp.",
                                    fontSize = 11.sp,
                                    color = NeutralMedium
                                )

                                if (logoBitmap != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
                                            shadowElevation = 2.dp,
                                            modifier = Modifier.size(70.dp)
                                        ) {
                                            Image(
                                                bitmap = logoBitmap.asImageBitmap(),
                                                contentDescription = "Logo Kafe",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilledTonalButton(
                                                onClick = {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Ganti Logo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.updateCafeLogoPath(null)
                                                    Toast.makeText(context, "Logo kafe dihapus", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = CoffeeRed, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Hapus Logo", color = CoffeeRed, fontSize = 11.5.sp)
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CoffeeBrownPrimary)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Unggah Logo Kafe (PNG / JPG)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    }
                                }
                            }
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
                                    saveStatus = "Profil dan struk kafe berhasil disimpan!"
                                    Toast.makeText(context, "Profil kafe berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                        ) {
                            Text("Simpan Profil & Struk Kafe")
                        }
                    }
                }
            }

            // Dedicated Tax & Service Charge Card
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
                            Icon(Icons.Filled.Percent, contentDescription = null, tint = BrightOrangePrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Pajak & Biaya Layanan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Text(
                            "Atur persentase pajak restoran (PB1) dan biaya layanan (service charge). Masukkan 0 jika tidak ada pajak.",
                            fontSize = 12.sp,
                            color = NeutralMedium
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = taxPercentage,
                                onValueChange = {
                                    taxPercentage = it
                                    taxSaveStatus = null
                                },
                                label = { Text("Pajak PB1 (%)") },
                                placeholder = { Text("10.0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("input_tax_percentage")
                            )
                            OutlinedTextField(
                                value = servicePercentage,
                                onValueChange = {
                                    servicePercentage = it
                                    taxSaveStatus = null
                                },
                                label = { Text("Service Charge (%)") },
                                placeholder = { Text("5.0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("input_service_percentage")
                            )
                        }

                        taxSaveStatus?.let {
                            Text(it, color = FreshGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.updateTaxAndService(taxPercentage, servicePercentage) { success, msg ->
                                    taxSaveStatus = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrightOrangePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_save_tax_service")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Simpan Pajak & Layanan", fontWeight = FontWeight.Bold)
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
                                    onClick = { showResetPasswordDialog = true },
                                    modifier = Modifier.testTag("btn_reset_pin_settings")
                                ) {
                                    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrightRed)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reset PIN", fontSize = 12.sp, color = BrightRed, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { showChangeMyPasswordDialog = true },
                                    modifier = Modifier.testTag("btn_change_my_pin_settings")
                                ) {
                                    Icon(
                                        Icons.Filled.Key,
                                        contentDescription = "Ganti PIN Saya",
                                        tint = BrightOrangePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
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
                                            userToDelete = user
                                        }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = CoffeeRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (currentUser?.role == "ADMIN") {
                            FilledTonalButton(
                                onClick = { showAddUserDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .testTag("btn_add_user_prominent")
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Tambah Akun Baru", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Backup & Restore Card (Keamanan & Cadangan Otomatis Bisnis)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Backup, contentDescription = null, tint = CoffeeBrownPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Cadangan Data & Inventaris", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAutoBackupEnabled) CoffeeGreenLight else NeutralLight
                            ) {
                                Text(
                                    text = if (isAutoBackupEnabled) "🛡️ AKTIF" else "NON-AKTIF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAutoBackupEnabled) CoffeeGreen else NeutralMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Auto-backup toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeutralLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Backup Data Otomatis", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text(
                                    "Otomatis mencadangkan stok bahan baku, resep, transaksi & pengaturan ke penyimpanan aman perangkat.",
                                    fontSize = 11.sp,
                                    color = NeutralMedium
                                )
                            }
                            Switch(
                                checked = isAutoBackupEnabled,
                                onCheckedChange = { viewModel.toggleAutoBackup(it) },
                                modifier = Modifier.testTag("switch_auto_backup")
                            )
                        }

                        // Last backup info
                        Surface(
                            color = PrimaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Status Cadangan Terakhir:", fontSize = 10.5.sp, color = NeutralMedium)
                                    Text(
                                        text = if (lastAutoBackupTime > 0) backupDateFormat.format(Date(lastAutoBackupTime)) else "Belum ada cadangan otomatis",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = CoffeeBrownDark
                                    )
                                }
                                Text(
                                    "${backupFiles.size} Berkas Tersimpan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CoffeeBrownPrimary
                                )
                            }
                        }

                        backupStatusMsg?.let { (success, msg) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (success) CoffeeGreenLight else CoffeeRedLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    color = if (success) CoffeeGreen else CoffeeRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        // Primary action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.triggerAutoBackupNow { success, msg ->
                                        backupStatusMsg = Pair(success, msg)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                                modifier = Modifier.weight(1f).testTag("btn_backup_now")
                            ) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cadangkan Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

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
                                        Toast.makeText(context, "Data JSON disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Salin JSON", fontSize = 11.5.sp)
                            }

                            OutlinedButton(
                                onClick = { showRestoreDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tempel JSON", fontSize = 11.5.sp)
                            }
                        }

                        // Available Backup Files List
                        if (backupFiles.isNotEmpty()) {
                            Text(
                                "Daftar Berkas Cadangan Tersimpan di Perangkat:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeutralDark
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                backupFiles.take(5).forEach { bFile ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NeutralLight,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    if (bFile.isAuto) Icons.Filled.CloudDone else Icons.Filled.InsertDriveFile,
                                                    contentDescription = null,
                                                    tint = if (bFile.isAuto) CoffeeGreen else CoffeeAmber,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        bFile.filename,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        "${backupDateFormat.format(Date(bFile.timestamp))} • ${bFile.sizeFormatted} • ${if (bFile.isAuto) "Otomatis" else "Manual"}",
                                                        fontSize = 10.sp,
                                                        color = NeutralMedium
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { viewModel.shareBackup(bFile.file) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.Share, contentDescription = "Bagikan", tint = CoffeeBrownPrimary, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { fileToRestore = bFile },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.Restore, contentDescription = "Pulihkan", tint = CoffeeGreen, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        exportedJson?.let { json ->
                            Surface(
                                color = NeutralLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp)
                            ) {
                                Text(
                                    text = json.take(250) + "...\n[Tersalin ke clipboard]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Kembali ke Pengaturan Awal (Factory Reset) Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeRed.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.RestartAlt,
                                    contentDescription = null,
                                    tint = CoffeeRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Kembali ke Pengaturan Awal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = CoffeeRed
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CoffeeRedLight
                            ) {
                                Text(
                                    text = "ZONA ADMIN",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoffeeRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Fitur ini akan menghapus dan mereset seluruh data aplikasi: semua laporan & riwayat transaksi, bahan baku & resep inventaris, produk menu & kategori, diskon voucher, serta data member.",
                            fontSize = 12.sp,
                            color = NeutralDark,
                            lineHeight = 17.sp
                        )

                        val isAdmin = currentUser?.role == "ADMIN"

                        if (!isAdmin) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeutralLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = NeutralMedium,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Tombol dikunci. Hanya akun Owner / Admin yang memiliki izin untuk mereset data.",
                                        fontSize = 11.5.sp,
                                        color = NeutralMedium
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (isAdmin) {
                                    resetAdminPinInput = ""
                                    resetPinError = null
                                    resetIncludeSampleData = true
                                    showResetToFactoryDialog = true
                                } else {
                                    Toast.makeText(context, "Hanya akun Admin yang dapat melakukan reset!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isAdmin,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoffeeRed,
                                disabledContainerColor = NeutralBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_reset_to_factory")
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Kembali ke Pengaturan Awal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
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
            title = { Text("Tambah Akun Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        placeholder = { Text("Contoh: kasir2 / manajer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama Lengkap") },
                        placeholder = { Text("Contoh: Siti Rahma") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("Password / PIN Masuk (4-6 Angka)") },
                        placeholder = { Text("1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Pilihan Role / Hak Akses:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeutralMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = newRole == "KASIR",
                            onClick = { newRole = "KASIR" },
                            label = { Text("Kasir") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newRole == "ADMIN",
                            onClick = { newRole = "ADMIN" },
                            label = { Text("Admin / Owner") },
                            modifier = Modifier.weight(1f)
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
                                        name = newName.trim().ifBlank { newUsername.trim() },
                                        pin = newPin.trim(),
                                        role = newRole
                                    )
                                )
                                Toast.makeText(context, "Akun '$newUsername' ($newRole) berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                showAddUserDialog = false
                            }
                        } else {
                            Toast.makeText(context, "Username wajib diisi & PIN minimal 4 angka!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                ) {
                    Text("Buat Akun", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) { Text("Batal") }
            }
        )
    }

    // Delete User Confirmation Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Konfirmasi Hapus Akun", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus akun pengguna '${user.name}' (Username: ${user.username}, Role: ${user.role})? Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.deleteUser(user)
                            Toast.makeText(context, "Akun '${user.name}' berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            userToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeRed)
                ) {
                    Text("Hapus Akun", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Batal")
                }
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

    // Reset Password / Lupa PIN Dialog
    if (showResetPasswordDialog) {
        ResetPasswordDialog(
            viewModel = viewModel,
            onDismiss = { showResetPasswordDialog = false }
        )
    }

    // Restore File Confirmation Dialog
    fileToRestore?.let { bFile ->
        AlertDialog(
            onDismissRequest = { fileToRestore = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = CoffeeOrange) },
            title = { Text("Pulihkan Cadangan?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Apakah Anda yakin ingin memulihkan database dari berkas:", fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(bFile.filename, fontWeight = FontWeight.Bold, color = CoffeeBrownPrimary, fontSize = 13.sp)
                    Text("Dibuat: ${backupDateFormat.format(Date(bFile.timestamp))} (${bFile.sizeFormatted})", fontSize = 11.5.sp, color = NeutralMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Peringatan: Data menu, stok bahan baku, dan transaksi akan diselaraskan dengan isi berkas cadangan ini.", fontSize = 11.5.sp, color = CoffeeRed)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackupFile(bFile.file) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            fileToRestore = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
                ) {
                    Text("Ya, Pulihkan Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRestore = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Reset ke Pengaturan Awal (Factory Reset) Confirmation & PIN Dialog
    if (showResetToFactoryDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResetting) {
                    showResetToFactoryDialog = false
                    resetPinError = null
                }
            },
            icon = {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = CoffeeRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "Kembali ke Pengaturan Awal?",
                    fontWeight = FontWeight.Bold,
                    color = CoffeeRed,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CoffeeRedLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "⚠️ PERINGATAN: Tindakan tidak dapat dibatalkan!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = CoffeeRed
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "• Semua laporan transaksi & kasir akan terhapus\n• Semua bahan baku & resep akan terhapus\n• Semua produk menu & kategori akan terhapus\n• Semua voucher & data member akan terhapus",
                                fontSize = 11.sp,
                                color = CoffeeRed
                            )
                        }
                    }

                    Text(
                        "Pilih mode pemulihan data setelah reset:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CoffeeBrownDark
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { resetIncludeSampleData = true }
                            .background(
                                if (resetIncludeSampleData) PrimaryContainer.copy(alpha = 0.5f) else NeutralLight.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = resetIncludeSampleData,
                            onClick = { resetIncludeSampleData = true }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Data Awal Bawaan (Default)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Muat menu & bahan baku contoh bawaan sistem", fontSize = 10.5.sp, color = NeutralMedium)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { resetIncludeSampleData = false }
                            .background(
                                if (!resetIncludeSampleData) PrimaryContainer.copy(alpha = 0.5f) else NeutralLight.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !resetIncludeSampleData,
                            onClick = { resetIncludeSampleData = false }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Bersih Total (Database Kosong)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Mulai dari nol tanpa menu/bahan baku contoh", fontSize = 10.5.sp, color = NeutralMedium)
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        "Konfirmasi PIN Admin:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CoffeeBrownDark
                    )

                    OutlinedTextField(
                        value = resetAdminPinInput,
                        onValueChange = {
                            resetAdminPinInput = it
                            resetPinError = null
                        },
                        label = { Text("Masukkan PIN Admin") },
                        placeholder = { Text("PIN Admin (misal: 1234)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_admin_reset_pin"),
                        isError = resetPinError != null
                    )

                    resetPinError?.let { err ->
                        Text(
                            text = err,
                            color = CoffeeRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (isResetting) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CoffeeRed)
                            Spacer(Modifier.width(8.dp))
                            Text("Mereset seluruh data...", fontSize = 11.5.sp, color = NeutralMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetAdminPinInput.isBlank()) {
                            resetPinError = "PIN Admin wajib diisi!"
                            return@Button
                        }
                        isResetting = true
                        viewModel.resetToFactoryDefaults(
                            enteredPin = resetAdminPinInput,
                            includeSampleData = resetIncludeSampleData
                        ) { success, msg ->
                            isResetting = false
                            if (success) {
                                coroutineScope.launch {
                                    cafeName = viewModel.repository.getSetting("cafe_name") ?: "Rencana Kopi"
                                    cafeAddress = viewModel.repository.getSetting("cafe_address") ?: "Jl. Malioboro No. 45, Yogyakarta"
                                    cafePhone = viewModel.repository.getSetting("cafe_phone") ?: "0812-3456-7890"
                                    receiptFooter = viewModel.repository.getSetting("receipt_footer") ?: "Terima kasih atas kunjungan Anda!\nFollow IG: @rencanakopi.id"
                                    taxPercentage = viewModel.repository.getSetting("tax_percentage") ?: "10.0"
                                    servicePercentage = viewModel.repository.getSetting("service_charge_percentage") ?: "5.0"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                showResetToFactoryDialog = false
                            } else {
                                resetPinError = msg
                            }
                        }
                    },
                    enabled = !isResetting,
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeRed),
                    modifier = Modifier.testTag("btn_confirm_factory_reset")
                ) {
                    Text("Ya, Hapus & Reset Semua", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetToFactoryDialog = false
                        resetPinError = null
                    },
                    enabled = !isResetting
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
