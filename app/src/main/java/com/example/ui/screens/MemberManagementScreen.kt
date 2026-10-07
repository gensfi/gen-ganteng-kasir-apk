package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MemberEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementScreen(
    viewModel: CafePosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsState()
    val pointsEarnRate by viewModel.pointsEarnRate.collectAsState()
    val pointsRedeemRate by viewModel.pointsRedeemRate.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<MemberEntity?>(null) }
    var memberToAdjustPoints by remember { mutableStateOf<MemberEntity?>(null) }
    var memberToDelete by remember { mutableStateOf<MemberEntity?>(null) }
    var showPointSettingsDialog by remember { mutableStateOf(false) }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    val filteredMembers = remember(members, searchQuery) {
        if (searchQuery.isBlank()) members
        else members.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery) ||
            (it.email?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val totalPoints = remember(members) { members.sumOf { it.points } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Member & Poin Loyalitas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Kelola pelanggan setia dan sistem poin belanja",
                            fontSize = 11.5.sp,
                            color = NeutralMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("member_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPointSettingsDialog = true },
                        modifier = Modifier.testTag("btn_point_settings")
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Pengaturan Poin", tint = CoffeeBrownPrimary)
                    }
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("btn_add_member")
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Tambah Member", tint = CoffeeGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats / Loyalty Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = CoffeeAmber.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = CoffeeAmber, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Program Poin Loyalitas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Perolehan & Penukaran Otomatis", fontSize = 11.sp, color = NeutralMedium)
                                }
                            }

                            FilledTonalButton(
                                onClick = { showPointSettingsDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Atur Rasio", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider(color = NeutralBorder.copy(alpha = 0.5f))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CoffeeCream,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Total Member", fontSize = 11.sp, color = CoffeeBrownDark, fontWeight = FontWeight.Medium)
                                    Text("${members.size}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CoffeeBrownPrimary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CoffeeAmberLight,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Total Poin Aktif", fontSize = 11.sp, color = CoffeeAmberDark, fontWeight = FontWeight.Medium)
                                    Text("$totalPoints Poin", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CoffeeAmber)
                                }
                            }
                        }

                        // Rule Display
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeutralLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = CoffeeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Belanja ${currency.format(pointsEarnRate)} = 1 Poin",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CoffeeBrownDark
                                    )
                                }
                                Text(
                                    "1 Poin = ${currency.format(pointsRedeemRate)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoffeeGreen
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar & Add Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama atau nomor HP...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = NeutralMedium)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Hapus")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("member_search_input")
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("btn_add_member_primary")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Member List Items
            if (filteredMembers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = NeutralMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isBlank()) "Belum ada member terdaftar" else "Member tidak ditemukan",
                                fontWeight = FontWeight.Bold,
                                color = NeutralMedium
                            )
                            Text(
                                "Daftarkan pelanggan setia untuk memberikan poin belanja",
                                fontSize = 12.sp,
                                color = NeutralMedium
                            )
                        }
                    }
                }
            } else {
                items(filteredMembers, key = { it.id }) { member ->
                    MemberItemCard(
                        member = member,
                        pointsRedeemRate = pointsRedeemRate,
                        currency = currency,
                        dateFormat = dateFormat,
                        onAdjustPoints = { memberToAdjustPoints = member },
                        onEdit = { memberToEdit = member },
                        onDelete = { memberToDelete = member }
                    )
                }
            }
        }
    }

    // Add Member Dialog
    if (showAddDialog) {
        AddEditMemberDialog(
            member = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, initialPoints, email, notes ->
                viewModel.addMember(name, phone, initialPoints, email, notes) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) showAddDialog = false
                }
            }
        )
    }

    // Edit Member Dialog
    memberToEdit?.let { member ->
        AddEditMemberDialog(
            member = member,
            onDismiss = { memberToEdit = null },
            onSave = { name, phone, points, email, notes ->
                val updated = member.copy(
                    name = name,
                    phone = phone,
                    points = points,
                    email = email,
                    notes = notes
                )
                viewModel.updateMember(updated) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) memberToEdit = null
                }
            }
        )
    }

    // Adjust / Remove Points Dialog
    memberToAdjustPoints?.let { member ->
        AdjustMemberPointsDialog(
            member = member,
            pointsRedeemRate = pointsRedeemRate,
            currency = currency,
            onDismiss = { memberToAdjustPoints = null },
            onAdjust = { delta ->
                viewModel.adjustMemberPoints(member.id, delta) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) memberToAdjustPoints = null
                }
            },
            onResetPoints = {
                viewModel.setMemberPoints(member.id, 0) { success, msg ->
                    Toast.makeText(context, "Semua poin member '${member.name}' berhasil dihapus (direset ke 0)!", Toast.LENGTH_SHORT).show()
                    if (success) memberToAdjustPoints = null
                }
            }
        )
    }

    // Delete Member Confirmation Dialog
    memberToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Hapus Member?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Yakin ingin menghapus member '${member.name}' (${member.phone})? Riwayat poin ${member.points} akan terhapus.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMember(member) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            memberToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeRed)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Point Settings Dialog
    if (showPointSettingsDialog) {
        PointSettingsDialog(
            currentEarnRate = pointsEarnRate,
            currentRedeemRate = pointsRedeemRate,
            currency = currency,
            onDismiss = { showPointSettingsDialog = false },
            onSave = { earnRate, redeemRate ->
                viewModel.savePointSettings(earnRate, redeemRate) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) showPointSettingsDialog = false
                }
            }
        )
    }
}

@Composable
fun MemberItemCard(
    member: MemberEntity,
    pointsRedeemRate: Double,
    currency: NumberFormat,
    dateFormat: SimpleDateFormat,
    onAdjustPoints: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = CoffeeCream,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                color = CoffeeBrownPrimary,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            member.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CoffeeBrownDark
                        )
                        Text(
                            "📱 ${member.phone}",
                            fontSize = 12.sp,
                            color = NeutralMedium
                        )
                        if (!member.email.isNullOrBlank()) {
                            Text(
                                "✉️ ${member.email}",
                                fontSize = 11.sp,
                                color = NeutralMedium
                            )
                        }
                    }
                }

                // Points Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CoffeeAmberLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeAmber.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = CoffeeAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${member.points} Poin",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = CoffeeAmberDark
                            )
                        }
                        Text(
                            "Nilai: ${currency.format(member.points * pointsRedeemRate)}",
                            fontSize = 10.sp,
                            color = CoffeeBrownDark
                        )
                    }
                }
            }

            if (!member.notes.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NeutralLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Catatan: ${member.notes}",
                        fontSize = 11.sp,
                        color = NeutralMedium,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Divider(color = NeutralBorder.copy(alpha = 0.5f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Terdaftar: ${dateFormat.format(Date(member.createdAt))}",
                    fontSize = 10.5.sp,
                    color = NeutralMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onAdjustPoints,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CoffeeAmberLight,
                            contentColor = CoffeeAmberDark
                        )
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kelola Poin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = CoffeeBrownPrimary, modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = CoffeeRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditMemberDialog(
    member: MemberEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, points: Int, email: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(member?.name ?: "") }
    var phone by remember { mutableStateOf(member?.phone ?: "") }
    var pointsText by remember { mutableStateOf(member?.points?.toString() ?: "0") }
    var email by remember { mutableStateOf(member?.email ?: "") }
    var notes by remember { mutableStateOf(member?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (member == null) "Daftar Member Baru" else "Edit Data Member", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap Member *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' } },
                    label = { Text("Nomor HP / WhatsApp *") },
                    placeholder = { Text("08xxxxxxxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { pointsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (member == null) "Poin Awal (Bonus Pendaftaran)" else "Saldo Poin") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Opsional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Pelanggan (Opsional)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        val pts = pointsText.toIntOrNull() ?: 0
                        onSave(name.trim(), phone.trim(), pts, email.trim().ifBlank { null }, notes.trim().ifBlank { null })
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
            ) {
                Text(if (member == null) "Daftarkan" else "Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AdjustMemberPointsDialog(
    member: MemberEntity,
    pointsRedeemRate: Double,
    currency: NumberFormat,
    onDismiss: () -> Unit,
    onAdjust: (delta: Int) -> Unit,
    onResetPoints: () -> Unit
) {
    var deltaInput by remember { mutableStateOf("") }
    var adjustMode by remember { mutableStateOf("ADD") } // "ADD", "DEDUCT", "RESET"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Kelola Poin Member", fontWeight = FontWeight.Bold)
                Text(
                    "${member.name} • Saldo: ${member.points} Poin",
                    fontSize = 12.sp,
                    color = CoffeeAmberDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = adjustMode == "ADD",
                        onClick = { adjustMode = "ADD" },
                        label = { Text("Tambah (+)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = adjustMode == "DEDUCT",
                        onClick = { adjustMode = "DEDUCT" },
                        label = { Text("Kurangi (-)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = adjustMode == "RESET",
                        onClick = { adjustMode = "RESET" },
                        label = { Text("Reset (0)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (adjustMode == "RESET") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CoffeeRedLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Perhatian: Opsi ini akan menghapus semua ${member.points} poin milik '${member.name}' dan mengembalikannya menjadi 0.",
                            color = CoffeeRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = deltaInput,
                        onValueChange = { deltaInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (adjustMode == "ADD") "Jumlah Poin yang Ditambahkan" else "Jumlah Poin yang Dikurangi") },
                        placeholder = { Text("Contoh: 50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10, 50, 100, 200).forEach { amount ->
                            FilterChip(
                                selected = deltaInput == amount.toString(),
                                onClick = { deltaInput = amount.toString() },
                                label = { Text("$amount") }
                            )
                        }
                    }

                    val inputVal = deltaInput.toIntOrNull() ?: 0
                    val newBalance = if (adjustMode == "ADD") member.points + inputVal else maxOf(0, member.points - inputVal)
                    Text(
                        "Saldo baru setelah diatur: $newBalance Poin (~${currency.format(newBalance * pointsRedeemRate)})",
                        fontSize = 11.5.sp,
                        color = NeutralMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (adjustMode == "RESET") {
                        onResetPoints()
                    } else {
                        val amount = deltaInput.toIntOrNull() ?: 0
                        if (amount > 0) {
                            val delta = if (adjustMode == "ADD") amount else -amount
                            onAdjust(delta)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (adjustMode == "RESET") CoffeeRed else CoffeeBrownPrimary
                )
            ) {
                Text(
                    when (adjustMode) {
                        "RESET" -> "Hapus Semua Poin"
                        "ADD" -> "Tambah Poin"
                        else -> "Kurangi Poin"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun PointSettingsDialog(
    currentEarnRate: Double,
    currentRedeemRate: Double,
    currency: NumberFormat,
    onDismiss: () -> Unit,
    onSave: (earnRate: Double, redeemRate: Double) -> Unit
) {
    var earnRateText by remember { mutableStateOf(currentEarnRate.toInt().toString()) }
    var redeemRateText by remember { mutableStateOf(currentRedeemRate.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pengaturan Rasio Poin", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Tentukan aturan perolehan poin belanja dan nilai tukar poin untuk diskon transaksi.",
                    fontSize = 12.sp,
                    color = NeutralMedium
                )

                // Earn Rate Input
                OutlinedTextField(
                    value = earnRateText,
                    onValueChange = { earnRateText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal Belanja untuk 1 Poin (Rp)") },
                    placeholder = { Text("Default: 200") },
                    supportingText = {
                        val earnVal = earnRateText.toDoubleOrNull() ?: 200.0
                        Text("Contoh: Belanja ${currency.format(earnVal)} dapat 1 poin")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Redeem Rate Input
                OutlinedTextField(
                    value = redeemRateText,
                    onValueChange = { redeemRateText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nilai Tukar 1 Poin ke Rupiah (Rp)") },
                    placeholder = { Text("Default: 1") },
                    supportingText = {
                        val redVal = redeemRateText.toDoubleOrNull() ?: 1.0
                        Text("Contoh: 100 poin bernilai potongan ${currency.format(100 * redVal)}")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Default Info Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CoffeeCream,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("💡 Aturan Sistem:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = CoffeeBrownDark)
                        Text("• Setiap pembelanjaan Rp200 mendapatkan 1 poin.", fontSize = 11.sp, color = CoffeeBrownDark)
                        Text("• Nilai Tukar: Setiap 1 Poin bernilai Rp1 saat checkout.", fontSize = 11.sp, color = CoffeeBrownDark)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val earn = earnRateText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 200.0
                    val redeem = redeemRateText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0
                    onSave(earn, redeem)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrownPrimary)
            ) {
                Text("Simpan Pengaturan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
