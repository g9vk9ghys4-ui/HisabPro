package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.theme.HisabGreen
import com.example.ui.theme.HisabGreenContainer
import com.example.ui.theme.HisabRed
import com.example.ui.theme.HisabRedContainer
import com.example.ui.viewmodel.HisabViewModel
import com.example.utils.BalanceStatus
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    viewModel: HisabViewModel,
    onBack: () -> Unit,
    onNavigateToParty: (Long) -> Unit
) {
    BackHandler { onBack() }
    var query by remember { mutableStateOf("") }

    val allParties by viewModel.allParties.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val partiesMap = remember(allParties) { allParties.associateBy { it.id } }

    val matchingParties = remember(query, allParties) {
        if (query.isBlank()) emptyList()
        else allParties.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.phone.contains(query, ignoreCase = true) ||
            it.businessName.contains(query, ignoreCase = true) ||
            it.notes.contains(query, ignoreCase = true)
        }
    }

    val matchingTransactions = remember(query, allTransactions, partiesMap) {
        if (query.isBlank()) emptyList()
        else allTransactions.filter { tx ->
            val party = partiesMap[tx.partyId]
            tx.description.contains(query, ignoreCase = true) ||
            tx.paymentMethod.contains(query, ignoreCase = true) ||
            tx.amount.toString().contains(query) ||
            (party?.name?.contains(query, ignoreCase = true) == true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search customers, amounts, notes...", fontSize = 14.sp) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("global_search_input")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (query.isBlank()) {
            EmptyStateView(
                icon = Icons.Default.Search,
                title = "Search HisabPro",
                description = "Type a customer name, phone number, payment mode, or description to search across all records.",
                modifier = Modifier.padding(innerPadding)
            )
        } else if (matchingParties.isEmpty() && matchingTransactions.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Search,
                title = "No results found for \"$query\"",
                description = "Check the spelling or try searching for another term.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (matchingParties.isNotEmpty()) {
                    item {
                        Text(
                            text = "CUSTOMERS & SUPPLIERS (${matchingParties.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(matchingParties, key = { "search_p_${it.id}" }) { party ->
                        val bal = CurrencyUtils.calculateBalance(party, allTransactions)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToParty(party.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PartyAvatar(name = party.name, isSupplier = party.isSupplier, sizeDp = 40)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(party.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${if (party.isSupplier) "Supplier" else "Customer"} • ${if (party.phone.isNotBlank()) party.phone else "No phone"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val statusColor = when (bal.status) {
                                        BalanceStatus.YOU_WILL_GET -> HisabGreen
                                        BalanceStatus.YOU_WILL_GIVE -> HisabRed
                                        BalanceStatus.SETTLED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(CurrencyUtils.format(bal.netAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = statusColor)
                                }
                            }
                        }
                    }
                }

                if (matchingTransactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "MATCHING TRANSACTIONS (${matchingTransactions.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(matchingTransactions, key = { "search_tx_${it.id}" }) { tx ->
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
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
                                        tint = color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(party?.name ?: "Customer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (tx.description.isNotBlank()) {
                                        Text(tx.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text("${DateUtils.formatDateTime(tx.dateMillis)} • ${tx.paymentMethod}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        (if (isGave) "[-] " else "[+] ") + CurrencyUtils.format(tx.amount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = color
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
