package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.screens.dialogs.AddEditPartyDialog
import com.example.ui.screens.dialogs.AddTransactionDialog
import com.example.ui.screens.dialogs.PaymentReceiptDialog
import com.example.ui.screens.dialogs.SetReminderDialog
import com.example.ui.theme.HisabAmber
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabGreen
import com.example.ui.theme.HisabGreenContainer
import com.example.ui.theme.HisabGreenOnContainer
import com.example.ui.theme.HisabRed
import com.example.ui.theme.HisabRedContainer
import com.example.ui.theme.HisabRedOnContainer
import com.example.ui.viewmodel.HisabViewModel
import com.example.utils.BalanceStatus
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.PdfHelper
import com.example.utils.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    partyId: Long,
    viewModel: HisabViewModel,
    onBack: () -> Unit,
    onViewStatement: (Long) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val parties by viewModel.allParties.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val lastCompletedTx by viewModel.lastCompletedTransaction.collectAsState()

    val party = parties.find { it.id == partyId }

    if (party == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Party not found", fontWeight = FontWeight.Bold)
        }
        return
    }

    val partyTransactions = allTransactions
        .filter { it.partyId == party.id }
        .sortedByDescending { it.dateMillis }

    val balance = CurrencyUtils.calculateBalance(party, partyTransactions)

    var showAddTxDialog by remember { mutableStateOf(false) }
    var txTypeToAdd by remember { mutableStateOf("GAVE") }
    var showEditPartyDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showWhatsAppReminderDialog by remember { mutableStateOf(false) }
    var reminderMessageDraft by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PartyAvatar(
                            name = party.name,
                            isSupplier = party.isSupplier,
                            sizeDp = 38
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = party.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (party.phone.isNotBlank()) {
                                Text(
                                    text = party.phone,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (party.phone.isNotBlank()) {
                        IconButton(
                            onClick = { ShareHelper.openDialer(context, party.phone) },
                            modifier = Modifier.testTag("detail_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = HisabBlue)
                        }
                        IconButton(
                            onClick = {
                                val defMsg = ShareHelper.buildReminderMessage(
                                    partyName = party.name,
                                    amountFormatted = CurrencyUtils.format(balance.netAmount),
                                    businessName = settings.businessName
                                )
                                reminderMessageDraft = defMsg
                                showWhatsAppReminderDialog = true
                            },
                            modifier = Modifier.testTag("detail_whatsapp_button")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = HisabGreen)
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Full Statement") },
                                onClick = {
                                    showMenu = false
                                    onViewStatement(party.id)
                                },
                                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Download PDF Statement") },
                                onClick = {
                                    showMenu = false
                                    PdfHelper.generateAndShareStatement(
                                        context = context,
                                        businessName = settings.businessName,
                                        businessPhone = settings.phone,
                                        party = party,
                                        transactions = partyTransactions,
                                        balance = balance
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Set Collection Date") },
                                onClick = {
                                    showMenu = false
                                    showReminderDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Party Details") },
                                onClick = {
                                    showMenu = false
                                    showEditPartyDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete Party", color = HisabRed) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = HisabRed) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Sticky bottom buttons: YOU GAVE vs YOU GOT
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            txTypeToAdd = "GAVE"
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HisabRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("you_gave_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YOU GAVE (₹)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            txTypeToAdd = "GOT"
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HisabGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("you_got_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YOU GOT (₹)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Balance Card
            item {
                val isCustomer = !party.isSupplier
                val (balanceTitle, statusColor, statusBg) = when (balance.status) {
                    BalanceStatus.YOU_WILL_GET -> Triple(
                        if (isCustomer) "YOU WILL GET" else "YOU GAVE ADVANCE",
                        HisabGreen,
                        HisabGreenContainer.copy(alpha = 0.5f)
                    )
                    BalanceStatus.YOU_WILL_GIVE -> Triple(
                        if (isCustomer) "YOU WILL GIVE (ADVANCE)" else "YOU WILL GIVE",
                        HisabRed,
                        HisabRedContainer.copy(alpha = 0.5f)
                    )
                    BalanceStatus.SETTLED -> Triple(
                        "SETTLED (NO DUES)",
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("balance_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = statusBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = balanceTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyUtils.format(balance.netAmount),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            modifier = Modifier.testTag("customer_balance_amount")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick action chips inside card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showReminderDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("set_collection_date_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (party.collectionDate != null) {
                                        "Due: " + DateUtils.formatShortDate(party.collectionDate)
                                    } else {
                                        "Collection Date"
                                    },
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { onViewStatement(party.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("view_statement_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Statement", fontSize = 11.sp)
                            }
                        }

                        // WhatsApp Reminder banner if balance is pending
                        if (balance.status == BalanceStatus.YOU_WILL_GET && balance.netAmount > 0.0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val defMsg = ShareHelper.buildReminderMessage(
                                        partyName = party.name,
                                        amountFormatted = CurrencyUtils.format(balance.netAmount),
                                        businessName = settings.businessName
                                    )
                                    reminderMessageDraft = defMsg
                                    showWhatsAppReminderDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HisabGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_whatsapp_reminder_button")
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send WhatsApp Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Transaction History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRANSACTION HISTORY (${partyTransactions.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "Total Gave: ${CurrencyUtils.format(balance.totalGave)} | Got: ${CurrencyUtils.format(balance.totalGot)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Transactions list
            if (partyTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Receipt,
                        title = "No transactions yet",
                        description = "Tap 'YOU GAVE' or 'YOU GOT' below to record the first entry with ${party.name}."
                    )
                }
            } else {
                // Calculate running balances
                // Chronological order ascending to compute running balances
                val chronological = partyTransactions.reversed()
                val runningBalancesMap = mutableMapOf<Long, Double>()
                var running = 0.0

                chronological.forEach { tx ->
                    if (!party.isSupplier) {
                        if (tx.type == "GAVE") running += tx.amount else running -= tx.amount
                    } else {
                        if (tx.type == "GOT") running += tx.amount else running -= tx.amount
                    }
                    runningBalancesMap[tx.id] = running
                }

                items(partyTransactions, key = { "detail_tx_${it.id}" }) { tx ->
                    val isGave = tx.type == "GAVE"
                    val runningVal = runningBalancesMap[tx.id] ?: 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon indicator
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isGave) HisabRedContainer else HisabGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isGave) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isGave) HisabRed else HisabGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isGave) "You Gave" else "You Got",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isGave) HisabRed else HisabGreen
                                )
                                if (tx.description.isNotBlank()) {
                                    Text(
                                        text = tx.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = DateUtils.formatDateTime(tx.dateMillis),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = " • ${tx.paymentMethod}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Amount & Balance
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyUtils.format(tx.amount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isGave) HisabRed else HisabGreen
                                )
                                Text(
                                    text = "Bal: ${CurrencyUtils.format(runningVal)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { transactionToDelete = tx },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete entry",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddTxDialog) {
        AddTransactionDialog(
            party = party,
            initialType = txTypeToAdd,
            onDismiss = { showAddTxDialog = false },
            onSave = { type, amount, description, paymentMethod, dateMillis ->
                viewModel.addTransaction(
                    partyId = party.id,
                    type = type,
                    amount = amount,
                    description = description,
                    paymentMethod = paymentMethod,
                    dateMillis = dateMillis
                )
            }
        )
    }

    // Receipt Dialog Popup
    if (lastCompletedTx != null && lastCompletedTx?.partyId == party.id) {
        val updatedPartyTransactions = (listOf(lastCompletedTx!!) + partyTransactions).distinctBy { it.id }
        val updatedBal = CurrencyUtils.calculateBalance(party, updatedPartyTransactions)
        val statusText = when (updatedBal.status) {
            BalanceStatus.YOU_WILL_GET -> "You'll Get ${CurrencyUtils.format(updatedBal.netAmount)}"
            BalanceStatus.YOU_WILL_GIVE -> "You'll Give ${CurrencyUtils.format(updatedBal.netAmount)}"
            BalanceStatus.SETTLED -> "Settled (₹0)"
        }

        PaymentReceiptDialog(
            transaction = lastCompletedTx!!,
            party = party,
            remainingBalanceStr = statusText,
            businessName = settings.businessName,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }

    // Set Reminder Dialog
    if (showReminderDialog) {
        SetReminderDialog(
            partyName = party.name,
            currentDate = party.collectionDate,
            onDismiss = { showReminderDialog = false },
            onSaveDate = { newDate ->
                viewModel.setCollectionDate(party.id, newDate)
            }
        )
    }

    // Edit Party Dialog
    if (showEditPartyDialog) {
        AddEditPartyDialog(
            initialParty = party,
            onDismiss = { showEditPartyDialog = false },
            onSave = { name, phone, address, businessName, notes, isSupplier ->
                viewModel.saveParty(
                    id = party.id,
                    name = name,
                    phone = phone,
                    address = address,
                    businessName = businessName,
                    notes = notes,
                    isSupplier = isSupplier
                )
            }
        )
    }

    // Confirm Delete Party Dialog
    if (showDeleteConfirmDialog) {
        ConfirmDeleteDialog(
            title = "Delete Party?",
            message = "Are you sure you want to delete ${party.name} and all their transaction history? This cannot be undone.",
            onConfirm = {
                viewModel.deleteParty(party) {
                    onBack()
                }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // Confirm Delete Transaction Dialog
    if (transactionToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Transaction?",
            message = "Are you sure you want to delete this ${CurrencyUtils.format(transactionToDelete!!.amount)} entry?",
            onConfirm = {
                viewModel.deleteTransaction(transactionToDelete!!)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }

    // WhatsApp Reminder Edit & Send Dialog
    if (showWhatsAppReminderDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppReminderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = HisabGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WhatsApp Reminder", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Review and edit the message before sending:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reminderMessageDraft,
                        onValueChange = { reminderMessageDraft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_message_input"),
                        minLines = 4,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ShareHelper.openWhatsApp(context, party.phone, reminderMessageDraft)
                        showWhatsAppReminderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HisabGreen),
                    modifier = Modifier.testTag("confirm_send_whatsapp_button")
                ) {
                    Text("Send on WhatsApp", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
