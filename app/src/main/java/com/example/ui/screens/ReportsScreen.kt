package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SummaryMetricCard
import com.example.ui.theme.HisabAmber
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabGreen
import com.example.ui.theme.HisabGreenContainer
import com.example.ui.theme.HisabRed
import com.example.ui.theme.HisabRedContainer
import com.example.ui.viewmodel.HisabViewModel
import com.example.utils.BalanceStatus
import com.example.utils.CurrencyUtils
import com.example.utils.DateFilter
import com.example.utils.DateUtils
import com.example.utils.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: HisabViewModel
) {
    val context = LocalContext.current
    var selectedDateFilter by remember { mutableStateOf(DateFilter.THIS_MONTH) }

    val allParties by viewModel.allParties.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allIncomes by viewModel.allIncomes.collectAsState()
    val allCashbook by viewModel.allCashbook.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val filteredTransactions = remember(allTransactions, selectedDateFilter) {
        allTransactions.filter { DateUtils.matchesFilter(it.dateMillis, selectedDateFilter) }
    }
    val filteredExpenses = remember(allExpenses, selectedDateFilter) {
        allExpenses.filter { DateUtils.matchesFilter(it.dateMillis, selectedDateFilter) }
    }
    val filteredIncomes = remember(allIncomes, selectedDateFilter) {
        allIncomes.filter { DateUtils.matchesFilter(it.dateMillis, selectedDateFilter) }
    }

    val totalGiven = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "GAVE" }.sumOf { it.amount }
    }
    val totalReceived = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "GOT" }.sumOf { it.amount }
    }

    val totalIncome = remember(filteredIncomes) { filteredIncomes.sumOf { it.amount } }
    val totalExpense = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }
    val netProfit = totalIncome - totalExpense

    // Outstanding Dues
    val partiesMap = remember(allParties) { allParties.associateBy { it.id } }
    val customersWithDues = remember(allParties, allTransactions) {
        allParties.filter { !it.isSupplier }.map { party ->
            val b = CurrencyUtils.calculateBalance(party, allTransactions)
            party to b
        }.filter { it.second.status == BalanceStatus.YOU_WILL_GET && it.second.netAmount > 0.0 }
        .sortedByDescending { it.second.netAmount }
    }

    val totalOutstanding = remember(customersWithDues) {
        customersWithDues.sumOf { it.second.netAmount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    IconButton(
                        onClick = {
                            val csv = ShareHelper.exportTransactionsCsv(allTransactions, partiesMap)
                            ShareHelper.shareText(context, "HisabPro Transactions CSV", csv)
                        },
                        modifier = Modifier.testTag("export_transactions_csv_button")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Export CSV", tint = HisabBlue)
                    }
                },
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
            // Date Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        DateFilter.ALL,
                        DateFilter.TODAY,
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

            // Outstanding / Due Report Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL OUTSTANDING DUES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(CurrencyUtils.format(totalOutstanding), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = HisabGreen)
                                Text("${customersWithDues.size} customer(s) have pending balances", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = {
                                    val csv = ShareHelper.exportPartiesCsv(allParties, allTransactions)
                                    ShareHelper.shareText(context, "Customers Due Report CSV", csv)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export CSV", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Visual: Money Given vs Received
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("MONEY GIVEN VS RECEIVED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        val totalMoneyMovement = totalGiven + totalReceived
                        val receivedRatio = if (totalMoneyMovement > 0) (totalReceived / totalMoneyMovement).toFloat() else 0.5f

                        // Visual ratio bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(HisabRedContainer)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = receivedRatio.coerceIn(0f, 1f))
                                    .height(12.dp)
                                    .background(HisabGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Received (CR)", fontSize = 11.sp, color = HisabGreen, fontWeight = FontWeight.Bold)
                                Text(CurrencyUtils.format(totalReceived), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HisabGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Given (DR)", fontSize = 11.sp, color = HisabRed, fontWeight = FontWeight.Bold)
                                Text(CurrencyUtils.format(totalGiven), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HisabRed)
                            }
                        }
                    }
                }
            }

            // Visual: Income vs Expense (Profitability)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("INCOME VS EXPENSE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        val totalBizFlow = totalIncome + totalExpense
                        val incomeRatio = if (totalBizFlow > 0) (totalIncome / totalBizFlow).toFloat() else 0.5f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(HisabRedContainer)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = incomeRatio.coerceIn(0f, 1f))
                                    .height(12.dp)
                                    .background(HisabGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Income", fontSize = 11.sp, color = HisabGreen, fontWeight = FontWeight.Bold)
                                Text(CurrencyUtils.format(totalIncome), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HisabGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val pColor = if (netProfit >= 0) HisabGreen else HisabRed
                                Text(if (netProfit >= 0) "Est. Profit" else "Est. Loss", fontSize = 11.sp, color = pColor, fontWeight = FontWeight.Bold)
                                Text(CurrencyUtils.format(netProfit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Expense", fontSize = 11.sp, color = HisabRed, fontWeight = FontWeight.Bold)
                                Text(CurrencyUtils.format(totalExpense), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HisabRed)
                            }
                        }
                    }
                }
            }

            // Top Outstanding Customers Table
            if (customersWithDues.isNotEmpty()) {
                item {
                    Text(
                        text = "TOP OUTSTANDING CUSTOMERS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(customersWithDues.take(5), key = { "top_due_${it.first.id}" }) { (party, balance) ->
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
                            Column {
                                Text(party.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (party.phone.isNotBlank()) {
                                    Text(party.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    CurrencyUtils.format(balance.netAmount),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HisabGreen
                                )
                                Text("You'll Get", fontSize = 10.sp, color = HisabGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}
