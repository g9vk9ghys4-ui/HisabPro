package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PartyEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.components.SummaryMetricCard
import com.example.ui.screens.dialogs.AddEditPartyDialog
import com.example.ui.screens.dialogs.AddTransactionDialog
import com.example.ui.theme.HisabAmber
import com.example.ui.theme.HisabAmberContainer
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabBlueContainer
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HisabViewModel,
    onNavigateToParty: (Long) -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val allParties by viewModel.allParties.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showSelectPartyForTxDialog by remember { mutableStateOf(false) }
    var selectedPartyForTx by remember { mutableStateOf<PartyEntity?>(null) }

    val customers = allParties.filter { !it.isSupplier }
    val partiesMap = remember(allParties) { allParties.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HisabPro",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = HisabBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HisabBlueContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "KHATA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HisabBlue
                                )
                            }
                        }
                        Text(
                            text = settings.businessName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("home_search_icon")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = HisabBlue)
                    }
                    IconButton(
                        onClick = onNavigateToReminders,
                        modifier = Modifier.testTag("home_notification_icon")
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Reminders", tint = HisabBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Summary Cards Row: YOU WILL GET & YOU WILL GIVE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = "You Will Get",
                        amountStr = CurrencyUtils.format(dashboardSummary.youWillGet),
                        subtitle = "Total receivable",
                        color = HisabGreen,
                        bgColor = HisabGreenContainer.copy(alpha = 0.5f),
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_you_will_get")
                    )

                    SummaryMetricCard(
                        title = "You Will Give",
                        amountStr = CurrencyUtils.format(dashboardSummary.youWillGive),
                        subtitle = "Total payable",
                        color = HisabRed,
                        bgColor = HisabRedContainer.copy(alpha = 0.5f),
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_you_will_give")
                    )
                }
            }

            // Secondary Summary: NET BALANCE & TODAY'S TRANSACTIONS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val netColor = if (dashboardSummary.netBalance >= 0) HisabGreen else HisabRed
                    val netBg = if (dashboardSummary.netBalance >= 0) HisabGreenContainer.copy(alpha = 0.25f) else HisabRedContainer.copy(alpha = 0.25f)

                    SummaryMetricCard(
                        title = "Net Balance",
                        amountStr = CurrencyUtils.format(dashboardSummary.netBalance),
                        subtitle = if (dashboardSummary.netBalance >= 0) "In your favor" else "You owe net",
                        color = netColor,
                        bgColor = netBg,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_net_balance")
                    )

                    SummaryMetricCard(
                        title = "Today's Txns",
                        amountStr = "${dashboardSummary.todayTransactionsCount}",
                        subtitle = "Rec: ${CurrencyUtils.format(dashboardSummary.todayMoneyReceived)}",
                        color = HisabBlue,
                        bgColor = HisabBlueContainer.copy(alpha = 0.4f),
                        icon = Icons.Default.ReceiptLong,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_today_transactions")
                    )
                }
            }

            // Quick Action Buttons: + ADD CUSTOMER & + ADD TRANSACTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showAddCustomerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = HisabBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_customer_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Customer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (allParties.isEmpty()) {
                                showAddCustomerDialog = true
                            } else {
                                showSelectPartyForTxDialog = true
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HisabBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_transaction_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Transaction", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Recent Customers Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOMERS (${customers.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    TextButton(onClick = onNavigateToCustomers) {
                        Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HisabBlue)
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = HisabBlue)
                    }
                }
            }

            if (customers.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.PersonAdd,
                        title = "No customers yet",
                        description = "Add your first customer to start tracking hisab & udhar.",
                        actionButtonText = "+ Add First Customer",
                        onActionClick = { showAddCustomerDialog = true }
                    )
                }
            } else {
                items(customers.take(5), key = { "cust_${it.id}" }) { customer ->
                    val balance = CurrencyUtils.calculateBalance(customer, allTransactions)
                    val (statusLabel, statusColor) = when (balance.status) {
                        BalanceStatus.YOU_WILL_GET -> "You'll Get" to HisabGreen
                        BalanceStatus.YOU_WILL_GIVE -> "You'll Give" to HisabRed
                        BalanceStatus.SETTLED -> "Settled" to MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToParty(customer.id) }
                            .testTag("customer_card_${customer.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PartyAvatar(name = customer.name, sizeDp = 42)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (customer.phone.isNotBlank()) customer.phone else "No phone",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyUtils.format(balance.netAmount),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                                Text(
                                    text = statusLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }

            // Recent Transactions Section
            item {
                Text(
                    text = "RECENT TRANSACTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            val recentTransactions = allTransactions.take(8)
            if (recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = "No transactions yet",
                        description = "When you record payments or credits, they will appear here in real time."
                    )
                }
            } else {
                items(recentTransactions, key = { "tx_${it.id}" }) { tx ->
                    val party = partiesMap[tx.partyId]
                    val isGave = tx.type == "GAVE"
                    val (typeLabel, typeColor) = if (isGave) {
                        "You Gave" to HisabRed
                    } else {
                        "You Got" to HisabGreen
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (party != null) onNavigateToParty(party.id)
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isGave) HisabRedContainer else HisabGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isGave) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = typeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = party?.name ?: "Customer",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (tx.description.isNotBlank()) tx.description else typeLabel,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${DateUtils.formatDateTime(tx.dateMillis)} • ${tx.paymentMethod}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyUtils.format(tx.amount),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = typeColor
                                )
                                Text(
                                    text = typeLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = typeColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddEditPartyDialog(
            defaultIsSupplier = false,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, address, businessName, notes, isSupplier ->
                viewModel.saveParty(
                    name = name,
                    phone = phone,
                    address = address,
                    businessName = businessName,
                    notes = notes,
                    isSupplier = isSupplier
                ) { newId ->
                    onNavigateToParty(newId)
                }
            }
        )
    }

    // Party picker dialog to add transaction
    if (showSelectPartyForTxDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSelectPartyForTxDialog = false },
            title = { Text("Select Customer or Supplier", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allParties, key = { "picker_${it.id}" }) { p ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSelectPartyForTxDialog = false
                                    selectedPartyForTx = p
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PartyAvatar(name = p.name, isSupplier = p.isSupplier, sizeDp = 32)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(if (p.isSupplier) "Supplier" else "Customer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSelectPartyForTxDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Transaction dialog after selecting party
    if (selectedPartyForTx != null) {
        AddTransactionDialog(
            party = selectedPartyForTx!!,
            initialType = "GAVE",
            onDismiss = { selectedPartyForTx = null },
            onSave = { type, amount, description, paymentMethod, dateMillis ->
                viewModel.addTransaction(
                    partyId = selectedPartyForTx!!.id,
                    type = type,
                    amount = amount,
                    description = description,
                    paymentMethod = paymentMethod,
                    dateMillis = dateMillis
                )
                selectedPartyForTx = null
            }
        )
    }
}
