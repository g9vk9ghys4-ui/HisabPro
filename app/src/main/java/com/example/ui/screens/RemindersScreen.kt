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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PartyEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyAvatar
import com.example.ui.theme.HisabAmber
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabGreen
import com.example.ui.theme.HisabRed
import com.example.ui.viewmodel.HisabViewModel
import com.example.utils.BalanceStatus
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: HisabViewModel,
    onNavigateToParty: (Long) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val parties by viewModel.allParties.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val now = System.currentTimeMillis()

    // Parties with collectionDate set and pending balance > 0
    val partiesWithReminders = parties.filter { it.collectionDate != null }.map { party ->
        val balance = CurrencyUtils.calculateBalance(party, transactions)
        Triple(party, balance, party.collectionDate!!)
    }.filter { it.second.status == BalanceStatus.YOU_WILL_GET && it.second.netAmount > 0.0 }

    val overdue = partiesWithReminders.filter { it.third < now && !DateUtils.isToday(it.third) }
    val today = partiesWithReminders.filter { DateUtils.isToday(it.third) }
    val upcoming = partiesWithReminders.filter { it.third > now && !DateUtils.isToday(it.third) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Reminders", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (partiesWithReminders.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Alarm,
                title = "No upcoming reminders",
                description = "Open any customer with a pending balance and tap 'Set Collection Date' to schedule a payment reminder.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (overdue.isNotEmpty()) {
                    item {
                        Text(
                            text = "OVERDUE COLLECTIONS (${overdue.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HisabRed,
                            letterSpacing = 0.5.sp
                        )
                    }
                    items(overdue, key = { "overdue_${it.first.id}" }) { (party, balance, dateMillis) ->
                        ReminderCard(
                            party = party,
                            balanceAmount = balance.netAmount,
                            dateMillis = dateMillis,
                            isOverdue = true,
                            onCardClick = { onNavigateToParty(party.id) },
                            onCall = { ShareHelper.openDialer(context, party.phone) },
                            onWhatsApp = {
                                val msg = ShareHelper.buildReminderMessage(
                                    partyName = party.name,
                                    amountFormatted = CurrencyUtils.format(balance.netAmount),
                                    businessName = settings.businessName
                                )
                                ShareHelper.openWhatsApp(context, party.phone, msg)
                            },
                            onMarkPaid = {
                                viewModel.addTransaction(
                                    partyId = party.id,
                                    type = "GOT",
                                    amount = balance.netAmount,
                                    description = "Payment cleared from reminder",
                                    paymentMethod = "Cash"
                                )
                                viewModel.setCollectionDate(party.id, null)
                            }
                        )
                    }
                }

                if (today.isNotEmpty()) {
                    item {
                        Text(
                            text = "TODAY'S COLLECTIONS (${today.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HisabAmber,
                            letterSpacing = 0.5.sp
                        )
                    }
                    items(today, key = { "today_${it.first.id}" }) { (party, balance, dateMillis) ->
                        ReminderCard(
                            party = party,
                            balanceAmount = balance.netAmount,
                            dateMillis = dateMillis,
                            isToday = true,
                            onCardClick = { onNavigateToParty(party.id) },
                            onCall = { ShareHelper.openDialer(context, party.phone) },
                            onWhatsApp = {
                                val msg = ShareHelper.buildReminderMessage(
                                    partyName = party.name,
                                    amountFormatted = CurrencyUtils.format(balance.netAmount),
                                    businessName = settings.businessName
                                )
                                ShareHelper.openWhatsApp(context, party.phone, msg)
                            },
                            onMarkPaid = {
                                viewModel.addTransaction(
                                    partyId = party.id,
                                    type = "GOT",
                                    amount = balance.netAmount,
                                    description = "Payment cleared from reminder",
                                    paymentMethod = "Cash"
                                )
                                viewModel.setCollectionDate(party.id, null)
                            }
                        )
                    }
                }

                if (upcoming.isNotEmpty()) {
                    item {
                        Text(
                            text = "UPCOMING COLLECTIONS (${upcoming.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }
                    items(upcoming, key = { "upcoming_${it.first.id}" }) { (party, balance, dateMillis) ->
                        ReminderCard(
                            party = party,
                            balanceAmount = balance.netAmount,
                            dateMillis = dateMillis,
                            onCardClick = { onNavigateToParty(party.id) },
                            onCall = { ShareHelper.openDialer(context, party.phone) },
                            onWhatsApp = {
                                val msg = ShareHelper.buildReminderMessage(
                                    partyName = party.name,
                                    amountFormatted = CurrencyUtils.format(balance.netAmount),
                                    businessName = settings.businessName
                                )
                                ShareHelper.openWhatsApp(context, party.phone, msg)
                            },
                            onMarkPaid = {
                                viewModel.addTransaction(
                                    partyId = party.id,
                                    type = "GOT",
                                    amount = balance.netAmount,
                                    description = "Payment cleared from reminder",
                                    paymentMethod = "Cash"
                                )
                                viewModel.setCollectionDate(party.id, null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    party: PartyEntity,
    balanceAmount: Double,
    dateMillis: Long,
    isOverdue: Boolean = false,
    isToday: Boolean = false,
    onCardClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onMarkPaid: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("reminder_card_${party.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PartyAvatar(name = party.name, isSupplier = party.isSupplier, sizeDp = 42)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(party.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = when {
                            isOverdue -> "Overdue: ${DateUtils.formatDate(dateMillis)}"
                            isToday -> "Due Today"
                            else -> "Due: ${DateUtils.formatDate(dateMillis)}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isOverdue -> HisabRed
                            isToday -> HisabAmber
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.format(balanceAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HisabGreen
                    )
                    Text("Pending", fontSize = 11.sp, color = HisabGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons: Call, WhatsApp, Mark as Paid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (party.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = onCall,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = HisabBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onWhatsApp,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = HisabGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onMarkPaid,
                    colors = ButtonDefaults.buttonColors(containerColor = HisabGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.4f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
