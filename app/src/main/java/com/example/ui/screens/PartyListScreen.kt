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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PartyEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.screens.dialogs.AddEditPartyDialog
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabGreen
import com.example.ui.theme.HisabRed
import com.example.ui.viewmodel.HisabViewModel
import com.example.utils.BalanceStatus
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.ShareHelper

enum class PartyFilter(val label: String) {
    ALL("All"),
    YOU_WILL_GET("You'll Get"),
    YOU_WILL_GIVE("You'll Give"),
    SETTLED("Settled")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    viewModel: HisabViewModel,
    isSupplierMode: Boolean = false,
    onNavigateToDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val allParties by viewModel.allParties.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PartyFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredByType = allParties.filter { it.isSupplier == isSupplierMode }

    val txGrouped = remember(allTransactions) {
        allTransactions.groupBy { it.partyId }
    }

    val filteredList = remember(filteredByType, txGrouped, searchQuery, selectedFilter) {
        filteredByType.filter { party ->
            val matchesSearch = searchQuery.isBlank() ||
                    party.name.contains(searchQuery, ignoreCase = true) ||
                    party.phone.contains(searchQuery, ignoreCase = true) ||
                    party.businessName.contains(searchQuery, ignoreCase = true)

            if (!matchesSearch) return@filter false

            val partyTxs = txGrouped[party.id] ?: emptyList()
            val balance = CurrencyUtils.calculateBalance(party, partyTxs)

            when (selectedFilter) {
                PartyFilter.ALL -> true
                PartyFilter.YOU_WILL_GET -> balance.status == BalanceStatus.YOU_WILL_GET
                PartyFilter.YOU_WILL_GIVE -> balance.status == BalanceStatus.YOU_WILL_GIVE
                PartyFilter.SETTLED -> balance.status == BalanceStatus.SETTLED
            }
        }
    }

    val pageTitle = if (isSupplierMode) "Suppliers" else "Customers"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "$pageTitle (${filteredByType.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = HisabBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_party_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add $pageTitle")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, phone or business...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("party_search_input")
            )

            // Filter Chips: ALL, YOU'LL GET, YOU'LL GIVE, SETTLED
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PartyFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Parties List
            if (filteredList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PersonAdd,
                    title = if (searchQuery.isNotBlank()) "No matching results" else "No $pageTitle yet",
                    description = if (searchQuery.isNotBlank()) "Try a different search term" else "Tap '+' to add your first $pageTitle.",
                    actionButtonText = if (searchQuery.isBlank()) "+ Add $pageTitle" else null,
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { "party_list_${it.id}" }) { party ->
                        val txs = txGrouped[party.id] ?: emptyList()
                        val balance = CurrencyUtils.calculateBalance(party, txs)
                        val lastTxDate = txs.maxByOrNull { it.dateMillis }?.dateMillis

                        val (statusText, statusColor) = when (balance.status) {
                            BalanceStatus.YOU_WILL_GET -> "You'll Get" to HisabGreen
                            BalanceStatus.YOU_WILL_GIVE -> "You'll Give" to HisabRed
                            BalanceStatus.SETTLED -> "Settled" to MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetail(party.id) }
                                .testTag("party_card_${party.id}"),
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
                                PartyAvatar(
                                    name = party.name,
                                    isSupplier = party.isSupplier,
                                    sizeDp = 44
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = party.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (party.businessName.isNotBlank()) {
                                        Text(
                                            text = party.businessName,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (party.phone.isNotBlank()) {
                                            Text(
                                                text = party.phone,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (lastTxDate != null) {
                                            Text(
                                                text = " • ${DateUtils.formatShortDate(lastTxDate)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyUtils.format(balance.netAmount),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor
                                    )
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Party Dialog
    if (showAddDialog) {
        AddEditPartyDialog(
            defaultIsSupplier = isSupplierMode,
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, address, businessName, notes, isSupplier ->
                viewModel.saveParty(
                    name = name,
                    phone = phone,
                    address = address,
                    businessName = businessName,
                    notes = notes,
                    isSupplier = isSupplier
                ) { newId ->
                    onNavigateToDetail(newId)
                }
            }
        )
    }
}
