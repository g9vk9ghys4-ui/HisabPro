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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CashbookEntryEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.components.SummaryMetricCard
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabBlueContainer
import com.example.ui.theme.HisabGreen
import com.example.ui.theme.HisabGreenContainer
import com.example.ui.theme.HisabRed
import com.example.ui.theme.HisabRedContainer
import com.example.ui.viewmodel.HisabViewModel
import com.example.utils.CurrencyUtils
import com.example.utils.DateFilter
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyCashbookScreen(
    viewModel: HisabViewModel,
    onNavigateToParty: (Long) -> Unit
) {
    var selectedMainTab by remember { mutableStateOf(0) } // 0: Money (All Txns), 1: Cashbook
    var selectedDateFilter by remember { mutableStateOf(DateFilter.ALL) }
    var selectedTxTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "GOT", "GAVE"

    val allTransactions by viewModel.allTransactions.collectAsState()
    val allParties by viewModel.allParties.collectAsState()
    val allCashbook by viewModel.allCashbook.collectAsState()
    val cashSummary by viewModel.cashSummary.collectAsState()

    val partiesMap = remember(allParties) { allParties.associateBy { it.id } }

    var showAddCashEntryDialog by remember { mutableStateOf(false) }
    var cashEntryTypeToAdd by remember { mutableStateOf("IN") }
    var entryToDelete by remember { mutableStateOf<CashbookEntryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Money & Cashbook", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Tabs: All Money vs Cashbook
            TabRow(
                selectedTabIndex = selectedMainTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = HisabBlue
            ) {
                Tab(
                    selected = selectedMainTab == 0,
                    onClick = { selectedMainTab = 0 },
                    text = { Text("MONEY MOVEMENT", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedMainTab == 1,
                    onClick = { selectedMainTab = 1 },
                    text = { Text("CASHBOOK", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            if (selectedMainTab == 0) {
                // MONEY MOVEMENT TAB
                val filteredTransactions = remember(allTransactions, selectedDateFilter, selectedTxTypeFilter) {
                    allTransactions.filter { tx ->
                        val matchesDate = DateUtils.matchesFilter(tx.dateMillis, selectedDateFilter)
                        val matchesType = when (selectedTxTypeFilter) {
                            "GOT" -> tx.type == "GOT"
                            "GAVE" -> tx.type == "GAVE"
                            else -> true
                        }
                        matchesDate && matchesType
                    }
                }

                val totalGot = remember(filteredTransactions) {
                    filteredTransactions.filter { it.type == "GOT" }.sumOf { it.amount }
                }
                val totalGave = remember(filteredTransactions) {
                    filteredTransactions.filter { it.type == "GAVE" }.sumOf { it.amount }
                }
                val netMovement = totalGot - totalGave

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Date Filter Chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                DateFilter.ALL,
                                DateFilter.TODAY,
                                DateFilter.YESTERDAY,
                                DateFilter.THIS_WEEK,
                                DateFilter.THIS_MONTH
                            ).forEach { filter ->
                                FilterChip(
                                    selected = selectedDateFilter == filter,
                                    onClick = { selectedDateFilter = filter },
                                    label = { Text(filter.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // Metrics: Total Received, Total Given, Net Movement
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryMetricCard(
                                title = "Total Received",
                                amountStr = CurrencyUtils.format(totalGot),
                                color = HisabGreen,
                                bgColor = HisabGreenContainer.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricCard(
                                title = "Total Given",
                                amountStr = CurrencyUtils.format(totalGave),
                                color = HisabRed,
                                bgColor = HisabRedContainer.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        val netColor = if (netMovement >= 0) HisabGreen else HisabRed
                        val netBg = if (netMovement >= 0) HisabGreenContainer.copy(alpha = 0.25f) else HisabRedContainer.copy(alpha = 0.25f)
                        SummaryMetricCard(
                            title = "Net Movement",
                            amountStr = CurrencyUtils.format(netMovement),
                            subtitle = if (netMovement >= 0) "Net Inflow" else "Net Outflow",
                            color = netColor,
                            bgColor = netBg,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Type Sub-tabs: ALL, YOU GOT, YOU GAVE
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedTxTypeFilter == "ALL",
                                onClick = { selectedTxTypeFilter = "ALL" },
                                label = { Text("ALL (${filteredTransactions.size})") }
                            )
                            FilterChip(
                                selected = selectedTxTypeFilter == "GOT",
                                onClick = { selectedTxTypeFilter = "GOT" },
                                label = { Text("YOU GOT") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HisabGreenContainer,
                                    selectedLabelColor = HisabGreen
                                )
                            )
                            FilterChip(
                                selected = selectedTxTypeFilter == "GAVE",
                                onClick = { selectedTxTypeFilter = "GAVE" },
                                label = { Text("YOU GAVE") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HisabRedContainer,
                                    selectedLabelColor = HisabRed
                                )
                            )
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.Receipt,
                                title = "No transactions found",
                                description = "There are no transactions matching the selected filters."
                            )
                        }
                    } else {
                        items(filteredTransactions, key = { "money_tx_${it.id}" }) { tx ->
                            val party = partiesMap[tx.partyId]
                            val isGave = tx.type == "GAVE"
                            val color = if (isGave) HisabRed else HisabGreen

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (party != null) onNavigateToParty(party.id)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isGave) HisabRedContainer else HisabGreenContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isGave) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(party?.name ?: "Customer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = if (tx.description.isNotBlank()) tx.description else (if (isGave) "Money Given" else "Money Received"),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${DateUtils.formatDateTime(tx.dateMillis)} • ${tx.paymentMethod}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = (if (isGave) "[-] " else "[+] ") + CurrencyUtils.format(tx.amount),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // CASHBOOK TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Cash In / Cash Out summary card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("CLOSING CASH IN HAND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = CurrencyUtils.format(cashSummary.closingBalance),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (cashSummary.closingBalance >= 0) HisabBlue else HisabRed
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = HisabBlue,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Cash In", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(CurrencyUtils.format(cashSummary.cashIn), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HisabGreen)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Cash Out", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(CurrencyUtils.format(cashSummary.cashOut), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HisabRed)
                                    }
                                }
                            }
                        }
                    }

                    // Add Cash Buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    cashEntryTypeToAdd = "IN"
                                    showAddCashEntryDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HisabGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("cash_in_button")
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Cash In", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    cashEntryTypeToAdd = "OUT"
                                    showAddCashEntryDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HisabRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("cash_out_button")
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("- Cash Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "CASH ENTRIES (${allCashbook.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (allCashbook.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.Payments,
                                title = "Cashbook is empty",
                                description = "Record cash received or paid directly, or add cash transactions to populate your cashbook."
                            )
                        }
                    } else {
                        // Calculate running balances
                        val chronological = allCashbook.reversed()
                        val runningMap = mutableMapOf<Long, Double>()
                        var runningCash = 0.0
                        chronological.forEach { c ->
                            if (c.type == "IN") runningCash += c.amount else runningCash -= c.amount
                            runningMap[c.id] = runningCash
                        }

                        items(allCashbook, key = { "cash_entry_${it.id}" }) { entry ->
                            val isIn = entry.type == "IN"
                            val color = if (isIn) HisabGreen else HisabRed
                            val runningVal = runningMap[entry.id] ?: 0.0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                            .background(if (isIn) HisabGreenContainer else HisabRedContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (entry.description.isNotBlank()) entry.description else (if (isIn) "Cash In" else "Cash Out"),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = DateUtils.formatDateTime(entry.dateMillis),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = (if (isIn) "[+] " else "[-] ") + CurrencyUtils.format(entry.amount),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                        Text(
                                            text = "Bal: ${CurrencyUtils.format(runningVal)}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { entryToDelete = entry },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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
    }

    // Add Cash Entry Dialog
    if (showAddCashEntryDialog) {
        var amountText by remember { mutableStateOf("") }
        var descText by remember { mutableStateOf("") }
        var isErr by remember { mutableStateOf(false) }

        val isIn = cashEntryTypeToAdd == "IN"

        AlertDialog(
            onDismissRequest = { showAddCashEntryDialog = false },
            title = {
                Text(
                    text = if (isIn) "Record Cash In (+)" else "Record Cash Out (-)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            if (isErr && it.isNotBlank()) isErr = false
                        },
                        label = { Text("Amount (₹) *") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = isErr,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("cash_amount_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("Description / Reason (e.g. Sales, Petty cash)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("cash_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = amountText.toDoubleOrNull()
                        if (parsed == null || parsed <= 0.0) {
                            isErr = true
                        } else {
                            viewModel.addCashbookEntry(
                                type = cashEntryTypeToAdd,
                                amount = parsed,
                                description = descText,
                                paymentMethod = "Cash"
                            )
                            showAddCashEntryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isIn) HisabGreen else HisabRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_cash_entry_button")
                ) {
                    Text(if (isIn) "Save Cash In" else "Save Cash Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCashEntryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Cash Entry Dialog
    if (entryToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Cash Entry?",
            message = "Are you sure you want to delete this cash entry?",
            onConfirm = {
                viewModel.deleteCashbookEntry(entryToDelete!!)
                entryToDelete = null
            },
            onDismiss = { entryToDelete = null }
        )
    }
}
