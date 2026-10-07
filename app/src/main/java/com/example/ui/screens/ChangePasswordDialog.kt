package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Password
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

@Composable
fun ChangePasswordDialog(
    viewModel: CafePosViewModel,
    targetUser: UserEntity? = null, // null means changing for currentUser
    isAdminReset: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val userToUpdate = targetUser ?: currentUser

    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    var oldPinVisible by remember { mutableStateOf(false) }
    var newPinVisible by remember { mutableStateOf(false) }
    var confirmPinVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
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

                Spacer(Modifier.height(12.dp))

                Text(
                    text = if (isAdminReset) "Reset PIN Kasir" else "Ganti PIN / Password",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeutralDark,
                        fontSize = 18.sp
                    )
                )

                Spacer(Modifier.height(4.dp))

                // User Identity Pill
                if (userToUpdate != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (userToUpdate.role == "ADMIN") BrightOrangePrimary.copy(alpha = 0.12f) else BrightTealLight,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = "Akun: ${userToUpdate.name} (${userToUpdate.role})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (userToUpdate.role == "ADMIN") BrightOrangeDark else BrightTeal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Error alert
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrightRedLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = BrightRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = BrightRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 1. Old PIN (Only required if not admin resetting another user)
                if (!isAdminReset) {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = {
                            oldPin = it
                            errorMessage = null
                        },
                        label = { Text("PIN / Password Lama") },
                        singleLine = true,
                        visualTransformation = if (oldPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        leadingIcon = {
                            Icon(Icons.Outlined.Password, contentDescription = null, tint = NeutralMedium)
                        },
                        trailingIcon = {
                            IconButton(onClick = { oldPinVisible = !oldPinVisible }) {
                                Icon(
                                    if (oldPinVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Tampilkan PIN",
                                    tint = NeutralMedium
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrightOrangePrimary,
                            unfocusedBorderColor = NeutralBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_old_pin")
                    )

                    Spacer(Modifier.height(10.dp))
                }

                // 2. New PIN
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        newPin = it
                        errorMessage = null
                    },
                    label = { Text("PIN Baru (4–6 Digit)") },
                    placeholder = { Text("Contoh: 1234") },
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
                                contentDescription = "Tampilkan PIN",
                                tint = NeutralMedium
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightOrangePrimary,
                        unfocusedBorderColor = NeutralBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_pin")
                )

                Spacer(Modifier.height(10.dp))

                // 3. Confirm New PIN
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
                                contentDescription = "Tampilkan PIN",
                                tint = NeutralMedium
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightOrangePrimary,
                        unfocusedBorderColor = NeutralBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_confirm_pin")
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Gunakan 4–6 digit angka yang mudah diingat untuk login kasir.",
                    fontSize = 11.sp,
                    color = NeutralMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_cancel_change_password")
                    ) {
                        Text("Batal", color = NeutralMedium)
                    }

                    Button(
                        onClick = {
                            if (!isAdminReset && oldPin.isBlank()) {
                                errorMessage = "Masukkan PIN lama Anda"
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
                            if (isAdminReset && userToUpdate != null) {
                                viewModel.adminResetUserPassword(userToUpdate, newPin) { success, msg ->
                                    isProcessing = false
                                    if (success) {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            } else {
                                viewModel.changeUserPassword(oldPin, newPin, confirmPin) { success, msg ->
                                    isProcessing = false
                                    if (success) {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrightOrangePrimary),
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_save_change_password")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Simpan PIN", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
