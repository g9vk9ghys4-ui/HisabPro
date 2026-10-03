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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SummaryMetricCard
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabGreen
import com.example.ui.theme.HisabGreenContainer
import com.example.ui.theme.HisabRed
import com.example.ui.theme.HisabRedContainer
import com.example.ui.viewmodel.HisabViewModel
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils

val expenseCategories = listOf(
    "Shop", "Transport", "Food", "Electricity", "Rent", "Salary", "Stock", "Repair", "Other"
)

val incomeCategories = listOf(
    "Sales", "Service", "Commission", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesIncomeScreen(
    viewModel: HisabViewModel
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Expenses, 1: Income, 2: Profit Summary
    val expenses by viewModel.allExpenses.collectAsState()
    val incomes by viewModel.allIncomes.collectAsState()
    val profitSummary by viewModel.profitSummary.collectAsState()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var incomeToDelete by remember { mutableStateOf<IncomeEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses & Income", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = HisabRed,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_expense_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            } else if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddIncomeDialog = true },
                    containerColor = HisabGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_income_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Income")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = HisabBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("EXPENSES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("INCOME", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("PROFIT / LOSS", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Expenses List & Metrics
                    val todayExpenses = expenses.filter { DateUtils.isToday(it.dateMillis) }.sumOf { it.amount }
                    val monthExpenses = expenses.filter { DateUtils.isThisMonth(it.dateMillis) }.sumOf { it.amount }
                    val totalExpenses = expenses.sumOf { it.amount }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SummaryMetricCard(
                                    title = "Today's Expense",
                                    amountStr = CurrencyUtils.format(todayExpenses),
                                    color = HisabRed,
                                    bgColor = HisabRedContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMetricCard(
                                    title = "This Month",
                                    amountStr = CurrencyUtils.format(monthExpenses),
                                    color = HisabRed,
                                    bgColor = HisabRedContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            SummaryMetricCard(
                                title = "Total Expenses",
                                amountStr = CurrencyUtils.format(totalExpenses),
                                color = HisabRed,
                                bgColor = HisabRedContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Text(
                                text = "EXPENSE LOG (${expenses.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (expenses.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.Receipt,
                                    title = "No expenses recorded",
                                    description = "Track shop rent, electricity, transport, stock or salaries here.",
                                    actionButtonText = "+ Add First Expense",
                                    onActionClick = { showAddExpenseDialog = true }
                                )
                            }
                        } else {
                            items(expenses, key = { "exp_${it.id}" }) { exp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(HisabRedContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = HisabRed, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(exp.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (exp.description.isNotBlank()) {
                                                Text(exp.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("${DateUtils.formatDateTime(exp.dateMillis)} • ${exp.paymentMethod}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(CurrencyUtils.format(exp.amount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HisabRed)
                                            IconButton(onClick = { expenseToDelete = exp }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Income List
                    val totalIncome = incomes.sumOf { it.amount }
                    val monthIncome = incomes.filter { DateUtils.isThisMonth(it.dateMillis) }.sumOf { it.amount }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SummaryMetricCard(
                                    title = "This Month",
                                    amountStr = CurrencyUtils.format(monthIncome),
                                    color = HisabGreen,
                                    bgColor = HisabGreenContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMetricCard(
                                    title = "Total Income",
                                    amountStr = CurrencyUtils.format(totalIncome),
                                    color = HisabGreen,
                                    bgColor = HisabGreenContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Text(
                                text = "INCOME LOG (${incomes.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (incomes.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.TrendingUp,
                                    title = "No income recorded",
                                    description = "Track sales, services, commission, or other business revenue.",
                                    actionButtonText = "+ Add First Income",
                                    onActionClick = { showAddIncomeDialog = true }
                                )
                            }
                        } else {
                            items(incomes, key = { "inc_${it.id}" }) { inc ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(HisabGreenContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = HisabGreen, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(inc.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (inc.description.isNotBlank()) {
                                                Text(inc.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("${DateUtils.formatDateTime(inc.dateMillis)} • ${inc.paymentMethod}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(CurrencyUtils.format(inc.amount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HisabGreen)
                                            IconButton(onClick = { incomeToDelete = inc }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Profit & Loss Summary Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val isProfit = profitSummary.estimatedProfit >= 0
                        val profitColor = if (isProfit) HisabGreen else HisabRed
                        val profitBg = if (isProfit) HisabGreenContainer.copy(alpha = 0.4f) else HisabRedContainer.copy(alpha = 0.4f)

                        item {
                            SummaryMetricCard(
                                title = if (isProfit) "Estimated Net Profit" else "Estimated Net Loss",
                                amountStr = CurrencyUtils.format(profitSummary.estimatedProfit),
                                subtitle = "Total Income - Total Expense",
                                color = profitColor,
                                bgColor = profitBg,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SummaryMetricCard(
                                    title = "Total Income",
                                    amountStr = CurrencyUtils.format(profitSummary.totalIncome),
                                    color = HisabGreen,
                                    bgColor = HisabGreenContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMetricCard(
                                    title = "Total Expense",
                                    amountStr = CurrencyUtils.format(profitSummary.totalExpense),
                                    color = HisabRed,
                                    bgColor = HisabRedContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("PROFITABILITY BREAKDOWN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val margin = if (profitSummary.totalIncome > 0) {
                                        ((profitSummary.estimatedProfit / profitSummary.totalIncome) * 100).toInt()
                                    } else 0
                                    Text("Profit Margin: $margin%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Keep your expenses lower than income to maintain healthy cash reserves.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        var category by remember { mutableStateOf(expenseCategories.first()) }
        var amountText by remember { mutableStateOf("") }
        var descText by remember { mutableStateOf("") }
        var isErr by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(expenseCategories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("Description / Notes") },
                        modifier = Modifier.fillMaxWidth()
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
                            viewModel.addExpense(category, parsed, descText, "Cash")
                            showAddExpenseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HisabRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_expense_button")
                ) {
                    Text("Save Expense", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Income Dialog
    if (showAddIncomeDialog) {
        var category by remember { mutableStateOf(incomeCategories.first()) }
        var amountText by remember { mutableStateOf("") }
        var descText by remember { mutableStateOf("") }
        var isErr by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddIncomeDialog = false },
            title = { Text("Add Income", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        incomeCategories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                        modifier = Modifier.fillMaxWidth().testTag("income_amount_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("Description / Client name") },
                        modifier = Modifier.fillMaxWidth()
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
                            viewModel.addIncome(category, parsed, descText, "Cash")
                            showAddIncomeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HisabGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_income_button")
                ) {
                    Text("Save Income", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIncomeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete Expense
    if (expenseToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Expense?",
            message = "Are you sure you want to delete this ${CurrencyUtils.format(expenseToDelete!!.amount)} expense entry?",
            onConfirm = {
                viewModel.deleteExpense(expenseToDelete!!)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }

    // Delete Income
    if (incomeToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Income?",
            message = "Are you sure you want to delete this ${CurrencyUtils.format(incomeToDelete!!.amount)} income entry?",
            onConfirm = {
                viewModel.deleteIncome(incomeToDelete!!)
                incomeToDelete = null
            },
            onDismiss = { incomeToDelete = null }
        )
    }
}
