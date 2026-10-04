package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.UserEntity
import com.example.ui.CafePosViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordDialog(
    viewModel: CafePosViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allUsers by viewModel.repository.getAllUsers().collectAsState(initial = emptyList())

    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
    var securityCode by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    var securityCodeVisible by remember { mutableStateOf(false) }
    var newPinVisible by remember { mutableStateOf(false) }
    var confirmPinVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(allUsers) {
        if (selectedUser == null && allUsers.isNotEmpty()) {
            selectedUser = allUsers.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.96f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(BrightPeach),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = null,
                        tint = BrightOrangePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Lupa PIN? Reset Password",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeutralDark,
                        fontSize = 18.sp
                    )
                )

                Text(
                    text = "Pilih akun dan masukkan Kode Keamanan Master untuk menyetel ulang PIN.",
                    fontSize = 11.5.sp,
                    color = NeutralMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Error alert
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrightRedLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = BrightRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = BrightRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 1. User Selection Pills
                Text(
                    text = "Pilih Akun yang akan di-reset:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeutralDark,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allUsers.forEach { user ->
                        val isSelected = selectedUser?.id == user.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BrightOrangePrimary else NeutralLight,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedUser = user
                                    errorMessage = null
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = user.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else NeutralDark
                                )
                                Text(
                                    text = user.role,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else NeutralMedium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 2. Master Security Code Input
                OutlinedTextField(
                    value = securityCode,
                    onValueChange = {
                        securityCode = it
                        errorMessage = null
                    },
                    label = { Text("Kode Keamanan Master (Default: 8888)") },
                    placeholder = { Text("8888") },
                    singleLine = true,
                    visualTransformation = if (securityCodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    leadingIcon = {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = BrightOrangePrimary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { securityCodeVisible = !securityCodeVisible }) {
                            Icon(
                                if (securityCodeVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = NeutralMedium
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightOrangePrimary,
                        unfocusedBorderColor = NeutralBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reset_security_code")
                )

                Spacer(Modifier.height(10.dp))

                // 3. New PIN
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        newPin = it
                        errorMessage = null
                    },
                    label = { Text("PIN Baru (4–6 Digit)") },
                    placeholder = { Text("Misal: 1234 atau 1111") },
                    singleLine = true,
                    visualTransformation = if (newPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    leadingIcon = {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = BrightOrangePrimary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { newPinVisible = !newPinVisible }) {
                            Icon(
                                if (newPinVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = NeutralMedium
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightOrangePrimary,
                        unfocusedBorderColor = NeutralBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reset_new_pin")
                )

                Spacer(Modifier.height(10.dp))

                // 4. Confirm PIN
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        confirmPin = it
                        errorMessage = null
                    },
                    label = { Text("Konfirmasi PIN Baru") },
                    singleLine = true,
                    visualTransformation = if (confirmPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    leadingIcon = {
                        Icon(Icons.Filled.CheckCircleOutline, contentDescription = null, tint = FreshGreen)
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPinVisible = !confirmPinVisible }) {
                            Icon(
                                if (confirmPinVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = NeutralMedium
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightOrangePrimary,
                        unfocusedBorderColor = NeutralBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reset_confirm_pin")
                )

                // Quick Preset: Reset to standard default button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val defaultPin = if (selectedUser?.role == "ADMIN") "1234" else "1111"
                            newPin = defaultPin
                            confirmPin = defaultPin
                        }
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrightOrangeDark)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Pakai PIN Standar (${if (selectedUser?.role == "ADMIN") "1234" else "1111"})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightOrangeDark
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Batal", color = NeutralMedium)
                    }

                    Button(
                        onClick = {
                            val user = selectedUser
                            if (user == null) {
                                errorMessage = "Pilih akun terlebih dahulu"
                                return@Button
                            }
                            if (securityCode.isBlank()) {
                                errorMessage = "Masukkan Kode Keamanan Master (8888)"
                                return@Button
                            }
                            if (newPin.trim().length < 4) {
                                errorMessage = "PIN baru minimal 4 angka / karakter"
                                return@Button
                            }
                            if (newPin.trim() != confirmPin.trim()) {
                                errorMessage = "Konfirmasi PIN baru tidak sesuai"
                                return@Button
                            }

                            isProcessing = true
                            viewModel.resetUserPassword(user, securityCode, newPin) { success, msg ->
                                isProcessing = false
                                if (success) {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrightOrangePrimary),
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_execute_reset_password")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Reset PIN", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
