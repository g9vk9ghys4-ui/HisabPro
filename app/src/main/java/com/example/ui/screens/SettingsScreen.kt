package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BusinessSettings
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabRed
import com.example.ui.viewmodel.HisabViewModel
import com.example.utils.ShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HisabViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val allParties by viewModel.allParties.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Business Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("BUSINESS PROFILE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { showEditProfileDialog = true }) {
                                Text("Edit", color = HisabBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(settings.businessName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Owner: ${settings.ownerName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (settings.phone.isNotBlank()) {
                            Text("Phone: ${settings.phone}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (settings.address.isNotBlank()) {
                            Text("Address: ${settings.address}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Security: PIN Lock
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("APP SECURITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = HisabBlue, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("4-Digit PIN Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = if (settings.isPinEnabled) "PIN lock active on app launch" else "Protect hisab with a PIN code",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.isPinEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        showPinDialog = true
                                    } else {
                                        viewModel.disablePin()
                                        Toast.makeText(context, "PIN Lock disabled", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("pin_lock_switch")
                            )
                        }
                    }
                }
            }

            // Language & Currency
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PREFERENCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Language selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = HisabBlue, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Language", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val languages = listOf("en" to "EN", "hi" to "हिन्दी", "bn" to "বাংলা")
                                languages.forEach { (code, name) ->
                                    val isSel = settings.language == code
                                    TextButton(
                                        onClick = {
                                            viewModel.updateSettings(settings.copy(language = code))
                                            Toast.makeText(context, "Language set to $name", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = name,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) HisabBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Currency
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Currency", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("₹ (INR - Indian Rupee)", fontWeight = FontWeight.Bold, color = HisabBlue)
                        }
                    }
                }
            }

            // Backup & Data Export
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("BACKUP & RESTORE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = viewModel.exportBackupJson()
                                    ShareHelper.shareText(context, "HisabPro Data Backup JSON", json)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("backup_json_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backup All Data (JSON)")
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("restore_json_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Data (JSON)")
                        }

                        OutlinedButton(
                            onClick = {
                                val partiesMap = allParties.associateBy { it.id }
                                val csv = ShareHelper.exportTransactionsCsv(allTransactions, partiesMap)
                                ShareHelper.shareText(context, "HisabPro Transactions CSV", csv)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Transactions CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                val csv = ShareHelper.exportPartiesCsv(allParties, allTransactions)
                                ShareHelper.shareText(context, "HisabPro Customers CSV", csv)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Customers CSV")
                        }
                    }
                }
            }

            // Danger Zone: Clear Data
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DANGER ZONE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HisabRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Clearing data will remove all customers, suppliers, transactions and logs from this device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = HisabRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("clear_all_data_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All Data", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var bName by remember { mutableStateOf(settings.businessName) }
        var oName by remember { mutableStateOf(settings.ownerName) }
        var phone by remember { mutableStateOf(settings.phone) }
        var addr by remember { mutableStateOf(settings.address) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Business Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bName,
                        onValueChange = { bName = it },
                        label = { Text("Business / Shop Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = oName,
                        onValueChange = { oName = it },
                        label = { Text("Owner Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addr,
                        onValueChange = { addr = it },
                        label = { Text("Address / City") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSettings(
                            settings.copy(
                                businessName = bName.trim(),
                                ownerName = oName.trim(),
                                phone = phone.trim(),
                                address = addr.trim()
                            )
                        )
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HisabBlue)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Set PIN Dialog
    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4) pin = it },
                        label = { Text("Enter 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4) confirmPin = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_new_pin_input")
                    )
                    if (pinError.isNotBlank()) {
                        Text(pinError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pin.length != 4) {
                            pinError = "PIN must be exactly 4 digits"
                        } else if (pin != confirmPin) {
                            pinError = "PINs do not match"
                        } else {
                            viewModel.setPin(pin)
                            showPinDialog = false
                            Toast.makeText(context, "PIN code enabled successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HisabBlue)
                ) {
                    Text("Enable PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Restore JSON Dialog
    if (showRestoreDialog) {
        var jsonInput by remember { mutableStateOf("") }
        var restoreError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Data from Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Paste the JSON backup string below to restore all your customer and transaction records:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = {
                            jsonInput = it
                            if (restoreError) restoreError = false
                        },
                        placeholder = { Text("{\"version\":1,\"appName\":\"HisabPro\"...}") },
                        minLines = 4,
                        maxLines = 8,
                        isError = restoreError,
                        supportingText = if (restoreError) { { Text("Invalid backup JSON format") } } else null,
                        modifier = Modifier.fillMaxWidth().testTag("restore_json_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = viewModel.restoreBackupJson(jsonInput)
                            if (success) {
                                showRestoreDialog = false
                                Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                restoreError = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HisabBlue)
                ) {
                    Text("Restore Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearConfirmDialog) {
        ConfirmDeleteDialog(
            title = "Erase All Data?",
            message = "This will permanently delete all customer accounts, transactions, expenses and cashbook entries from this device. Are you completely sure?",
            onConfirm = {
                viewModel.clearAllData {
                    Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }
}
