package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HisabBlue
import com.example.ui.viewmodel.HisabViewModel

sealed class Screen {
    object Home : Screen()
    object Customers : Screen()
    object Suppliers : Screen()
    object Money : Screen()
    object Reports : Screen()
    object ExpensesIncome : Screen()
    object Reminders : Screen()
    object Settings : Screen()
    object Search : Screen()
    data class CustomerDetail(val partyId: Long) : Screen()
    data class Statement(val partyId: Long) : Screen()
}

enum class BottomNavItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Default.Home, "nav_home"),
    CUSTOMERS("Customers", Icons.Default.People, "nav_customers"),
    SUPPLIERS("Suppliers", Icons.Default.Business, "nav_suppliers"),
    MONEY("Money", Icons.Default.Payments, "nav_money"),
    REPORTS("Reports", Icons.Default.ReceiptLong, "nav_reports"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

@Composable
fun MainAppScaffold(
    viewModel: HisabViewModel
) {
    val isLocked by viewModel.isAppLocked.collectAsState()

    if (isLocked) {
        PinLockScreen(viewModel = viewModel)
        return
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    val activeBottomNav = when (currentScreen) {
        Screen.Home -> BottomNavItem.HOME
        Screen.Customers -> BottomNavItem.CUSTOMERS
        Screen.Suppliers -> BottomNavItem.SUPPLIERS
        Screen.Money -> BottomNavItem.MONEY
        Screen.Reports -> BottomNavItem.REPORTS
        Screen.Settings, Screen.ExpensesIncome, Screen.Reminders -> BottomNavItem.SETTINGS
        else -> null
    }

    // Top-level back handler
    BackHandler(enabled = currentScreen != Screen.Home) {
        when (currentScreen) {
            is Screen.CustomerDetail -> currentScreen = Screen.Customers
            is Screen.Statement -> {
                val pId = (currentScreen as Screen.Statement).partyId
                currentScreen = Screen.CustomerDetail(pId)
            }
            Screen.ExpensesIncome, Screen.Reminders, Screen.Settings, Screen.Search -> {
                currentScreen = Screen.Home
            }
            else -> currentScreen = Screen.Home
        }
    }

    val isTopLevelScreen = currentScreen is Screen.Home ||
            currentScreen is Screen.Customers ||
            currentScreen is Screen.Suppliers ||
            currentScreen is Screen.Money ||
            currentScreen is Screen.Reports ||
            currentScreen is Screen.Settings

    if (isTabletOrLandscape) {
        // Tablet / Expanded screen layout with NavigationRail
        Row(modifier = Modifier.fillMaxSize()) {
            if (isTopLevelScreen) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = HisabBlue
                ) {
                    BottomNavItem.values().forEach { item ->
                        val selected = activeBottomNav == item
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                currentScreen = when (item) {
                                    BottomNavItem.HOME -> Screen.Home
                                    BottomNavItem.CUSTOMERS -> Screen.Customers
                                    BottomNavItem.SUPPLIERS -> Screen.Suppliers
                                    BottomNavItem.MONEY -> Screen.Money
                                    BottomNavItem.REPORTS -> Screen.Reports
                                    BottomNavItem.SETTINGS -> Screen.Settings
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 11.sp) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                ScreenContent(
                    currentScreen = currentScreen,
                    viewModel = viewModel,
                    onNavigate = { currentScreen = it }
                )
            }
        }
    } else {
        // Mobile layout with Bottom NavigationBar
        Scaffold(
            bottomBar = {
                if (isTopLevelScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        BottomNavItem.values().forEach { item ->
                            val selected = activeBottomNav == item
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    currentScreen = when (item) {
                                        BottomNavItem.HOME -> Screen.Home
                                        BottomNavItem.CUSTOMERS -> Screen.Customers
                                        BottomNavItem.SUPPLIERS -> Screen.Suppliers
                                        BottomNavItem.MONEY -> Screen.Money
                                        BottomNavItem.REPORTS -> Screen.Reports
                                        BottomNavItem.SETTINGS -> Screen.Settings
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ScreenContent(
                    currentScreen = currentScreen,
                    viewModel = viewModel,
                    onNavigate = { currentScreen = it }
                )
            }
        }
    }
}

@Composable
private fun ScreenContent(
    currentScreen: Screen,
    viewModel: HisabViewModel,
    onNavigate: (Screen) -> Unit
) {
    when (currentScreen) {
        Screen.Home -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToParty = { onNavigate(Screen.CustomerDetail(it)) },
                onNavigateToCustomers = { onNavigate(Screen.Customers) },
                onNavigateToReminders = { onNavigate(Screen.Reminders) },
                onNavigateToSearch = { onNavigate(Screen.Search) }
            )
        }
        Screen.Customers -> {
            CustomerListScreen(
                viewModel = viewModel,
                isSupplierMode = false,
                onNavigateToDetail = { onNavigate(Screen.CustomerDetail(it)) }
            )
        }
        Screen.Suppliers -> {
            CustomerListScreen(
                viewModel = viewModel,
                isSupplierMode = true,
                onNavigateToDetail = { onNavigate(Screen.CustomerDetail(it)) }
            )
        }
        Screen.Money -> {
            MoneyCashbookScreen(
                viewModel = viewModel,
                onNavigateToParty = { onNavigate(Screen.CustomerDetail(it)) }
            )
        }
        Screen.Reports -> {
            ReportsScreen(viewModel = viewModel)
        }
        Screen.Settings -> {
            SettingsScreen(viewModel = viewModel)
        }
        Screen.ExpensesIncome -> {
            ExpensesIncomeScreen(viewModel = viewModel)
        }
        Screen.Reminders -> {
            RemindersScreen(
                viewModel = viewModel,
                onNavigateToParty = { onNavigate(Screen.CustomerDetail(it)) },
                onBack = { onNavigate(Screen.Home) }
            )
        }
        Screen.Search -> {
            GlobalSearchScreen(
                viewModel = viewModel,
                onBack = { onNavigate(Screen.Home) },
                onNavigateToParty = { onNavigate(Screen.CustomerDetail(it)) }
            )
        }
        is Screen.CustomerDetail -> {
            CustomerDetailScreen(
                partyId = currentScreen.partyId,
                viewModel = viewModel,
                onBack = { onNavigate(Screen.Customers) },
                onViewStatement = { onNavigate(Screen.Statement(it)) }
            )
        }
        is Screen.Statement -> {
            CustomerStatementScreen(
                partyId = currentScreen.partyId,
                viewModel = viewModel,
                onBack = { onNavigate(Screen.CustomerDetail(currentScreen.partyId)) }
            )
        }
    }
}
