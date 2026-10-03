package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabGreen
import com.example.ui.theme.HisabRed
import com.example.ui.viewmodel.HisabViewModel
import com.example.utils.BalanceStatus
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.PdfHelper
import com.example.utils.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerStatementScreen(
    partyId: Long,
    viewModel: HisabViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val parties by viewModel.allParties.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val party = parties.find { it.id == partyId }

    if (party == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Party not found")
        }
        return
    }

    val partyTransactions = allTransactions
        .filter { it.partyId == party.id }
        .sortedBy { it.dateMillis }

    val balance = CurrencyUtils.calculateBalance(party, partyTransactions)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Statement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("statement_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            PdfHelper.generateAndShareStatement(
                                context = context,
                                businessName = settings.businessName,
                                businessPhone = settings.phone,
                                party = party,
                                transactions = partyTransactions,
                                balance = balance
                            )
                        },
                        modifier = Modifier.testTag("statement_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = HisabBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = ShareHelper.buildStatementSummary(
                                party = party,
                                transactions = partyTransactions,
                                balance = balance,
                                businessName = settings.businessName
                            )
                            ShareHelper.shareText(context, "Account Statement - ${party.name}", text)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_statement_text_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val text = ShareHelper.buildStatementSummary(
                                party = party,
                                transactions = partyTransactions,
                                balance = balance,
                                businessName = settings.businessName
                            )
                            ShareHelper.openWhatsApp(context, party.phone, text)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("share_statement_whatsapp_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = HisabGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            PdfHelper.generateAndShareStatement(
                                context = context,
                                businessName = settings.businessName,
                                businessPhone = settings.phone,
                                party = party,
                                transactions = partyTransactions,
                                balance = balance
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HisabBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("download_statement_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Business Statement Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "HisabPro",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = HisabBlue
                                )
                                Text(
                                    text = "Simple business. Clear hisab.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = settings.businessName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (settings.phone.isNotBlank()) {
                                    Text(
                                        text = settings.phone,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = DateUtils.formatDate(System.currentTimeMillis()),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("STATEMENT FOR:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(party.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                if (party.phone.isNotBlank()) {
                                    Text("Phone: ${party.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (party.businessName.isNotBlank()) {
                                    Text("Business: ${party.businessName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("NET BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val statusLabel = when (balance.status) {
                                    BalanceStatus.YOU_WILL_GET -> "You'll Get"
                                    BalanceStatus.YOU_WILL_GIVE -> "You'll Give"
                                    BalanceStatus.SETTLED -> "Settled"
                                }
                                val statusColor = when (balance.status) {
                                    BalanceStatus.YOU_WILL_GET -> HisabGreen
                                    BalanceStatus.YOU_WILL_GIVE -> HisabRed
                                    BalanceStatus.SETTLED -> MaterialTheme.colorScheme.onSurface
                                }
                                Text(CurrencyUtils.format(balance.netAmount), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = statusColor)
                                Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                            }
                        }
                    }
                }
            }

            // Totals Summary Box
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("TOTAL GAVE (DR)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HisabRed)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(CurrencyUtils.format(balance.totalGave), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HisabRed)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("TOTAL RECEIVED (CR)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HisabGreen)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(CurrencyUtils.format(balance.totalGot), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HisabGreen)
                        }
                    }
                }
            }

            // Statement Table
            item {
                Text(
                    text = "TRANSACTION BREAKDOWN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            var running = 0.0
            items(partyTransactions, key = { "stmt_tx_${it.id}" }) { tx ->
                val isGave = tx.type == "GAVE"
                if (!party.isSupplier) {
                    if (isGave) running += tx.amount else running -= tx.amount
                } else {
                    if (isGave) running -= tx.amount else running += tx.amount
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(DateUtils.formatDateTime(tx.dateMillis), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (tx.description.isNotBlank()) tx.description else (if (isGave) "Money Given" else "Money Received"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text("Mode: ${tx.paymentMethod}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = (if (isGave) "[-] " else "[+] ") + CurrencyUtils.format(tx.amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGave) HisabRed else HisabGreen
                            )
                            Text("Bal: ${CurrencyUtils.format(running)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
