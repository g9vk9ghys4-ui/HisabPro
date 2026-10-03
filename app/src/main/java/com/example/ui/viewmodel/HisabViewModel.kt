package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.BusinessSettings
import com.example.data.entity.CashbookEntryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.PartyEntity
import com.example.data.entity.SettingsManager
import com.example.data.entity.TransactionEntity
import com.example.data.repository.HisabRepository
import com.example.utils.BalanceStatus
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardSummary(
    val youWillGet: Double = 0.0,
    val youWillGive: Double = 0.0,
    val netBalance: Double = 0.0,
    val todayTransactionsCount: Int = 0,
    val todayMoneyReceived: Double = 0.0,
    val todayMoneyGiven: Double = 0.0
)

data class CashSummary(
    val openingBalance: Double = 0.0,
    val cashIn: Double = 0.0,
    val cashOut: Double = 0.0,
    val closingBalance: Double = 0.0
)

data class ProfitSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val estimatedProfit: Double = 0.0
)

class HisabViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = HisabRepository(database)
    private val settingsManager = SettingsManager(application)

    private val _settings = MutableStateFlow(settingsManager.getSettings())
    val settings: StateFlow<BusinessSettings> = _settings.asStateFlow()

    private val _isAppLocked = MutableStateFlow(settingsManager.getSettings().isPinEnabled)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    val allParties: StateFlow<List<PartyEntity>> = repository.getAllParties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCashbook: StateFlow<List<CashbookEntryEntity>> = repository.getAllCashbookEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allIncomes: StateFlow<List<IncomeEntity>> = repository.getAllIncomes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard calculations
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        allParties,
        allTransactions
    ) { parties, transactions ->
        var totalGet = 0.0
        var totalGive = 0.0
        var todayCount = 0
        var todayGot = 0.0
        var todayGave = 0.0

        parties.forEach { party ->
            val b = CurrencyUtils.calculateBalance(party, transactions)
            if (b.status == BalanceStatus.YOU_WILL_GET) {
                totalGet += b.netAmount
            } else if (b.status == BalanceStatus.YOU_WILL_GIVE) {
                totalGive += b.netAmount
            }
        }

        transactions.forEach { tx ->
            if (DateUtils.isToday(tx.dateMillis)) {
                todayCount++
                if (tx.type == "GOT") todayGot += tx.amount
                if (tx.type == "GAVE") todayGave += tx.amount
            }
        }

        DashboardSummary(
            youWillGet = totalGet,
            youWillGive = totalGive,
            netBalance = totalGet - totalGive,
            todayTransactionsCount = todayCount,
            todayMoneyReceived = todayGot,
            todayMoneyGiven = todayGave
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    // Cashbook summary
    val cashSummary: StateFlow<CashSummary> = allCashbook.combine(allCashbook) { list, _ ->
        var totalIn = 0.0
        var totalOut = 0.0
        list.forEach {
            if (it.type == "IN") totalIn += it.amount else totalOut += it.amount
        }
        CashSummary(
            openingBalance = 0.0,
            cashIn = totalIn,
            cashOut = totalOut,
            closingBalance = totalIn - totalOut
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashSummary())

    // Profit summary
    val profitSummary: StateFlow<ProfitSummary> = combine(
        allIncomes,
        allExpenses
    ) { incList, expList ->
        val incTotal = incList.sumOf { it.amount }
        val expTotal = expList.sumOf { it.amount }
        ProfitSummary(
            totalIncome = incTotal,
            totalExpense = expTotal,
            estimatedProfit = incTotal - expTotal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfitSummary())

    // Last added transaction for receipt dialog popup
    private val _lastCompletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val lastCompletedTransaction: StateFlow<TransactionEntity?> = _lastCompletedTransaction.asStateFlow()

    fun dismissReceipt() {
        _lastCompletedTransaction.value = null
    }

    // Party CRUD
    fun saveParty(
        id: Long = 0L,
        name: String,
        phone: String = "",
        address: String = "",
        businessName: String = "",
        notes: String = "",
        isSupplier: Boolean = false,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val party = PartyEntity(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                businessName = businessName.trim(),
                notes = notes.trim(),
                isSupplier = isSupplier
            )
            val savedId = repository.saveParty(party)
            onComplete(savedId)
        }
    }

    fun deleteParty(party: PartyEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteParty(party)
            onComplete()
        }
    }

    fun setCollectionDate(partyId: Long, dateMillis: Long?) {
        viewModelScope.launch {
            repository.updateCollectionDate(partyId, dateMillis)
        }
    }

    // Transaction CRUD
    fun addTransaction(
        partyId: Long,
        type: String, // "GAVE" or "GOT"
        amount: Double,
        description: String,
        paymentMethod: String = "Cash",
        dateMillis: Long = System.currentTimeMillis(),
        showReceipt: Boolean = true,
        onComplete: (TransactionEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                partyId = partyId,
                type = type,
                amount = amount,
                description = description.trim(),
                paymentMethod = paymentMethod,
                dateMillis = dateMillis
            )
            val newId = repository.addTransaction(tx)
            val savedTx = tx.copy(id = newId)
            if (showReceipt) {
                _lastCompletedTransaction.value = savedTx
            }
            onComplete(savedTx)
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    // Cashbook CRUD
    fun addCashbookEntry(
        type: String,
        amount: Double,
        description: String,
        paymentMethod: String = "Cash",
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addCashbookEntry(
                CashbookEntryEntity(
                    type = type,
                    amount = amount,
                    description = description.trim(),
                    paymentMethod = paymentMethod,
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun deleteCashbookEntry(entry: CashbookEntryEntity) {
        viewModelScope.launch {
            repository.deleteCashbookEntry(entry)
        }
    }

    // Expense CRUD
    fun addExpense(
        category: String,
        amount: Double,
        description: String,
        paymentMethod: String = "Cash",
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addExpense(
                ExpenseEntity(
                    category = category,
                    amount = amount,
                    description = description.trim(),
                    paymentMethod = paymentMethod,
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // Income CRUD
    fun addIncome(
        category: String,
        amount: Double,
        description: String,
        paymentMethod: String = "Cash",
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addIncome(
                IncomeEntity(
                    category = category,
                    amount = amount,
                    description = description.trim(),
                    paymentMethod = paymentMethod,
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch {
            repository.deleteIncome(income)
        }
    }

    // Settings
    fun updateSettings(newSettings: BusinessSettings) {
        settingsManager.saveSettings(newSettings)
        _settings.value = newSettings
    }

    fun setPin(pin: String) {
        settingsManager.setPin(pin)
        _settings.value = settingsManager.getSettings()
    }

    fun disablePin() {
        settingsManager.disablePin()
        _settings.value = settingsManager.getSettings()
        _isAppLocked.value = false
    }

    fun unlockApp(pin: String): Boolean {
        val ok = settingsManager.verifyPin(pin)
        if (ok) {
            _isAppLocked.value = false
        }
        return ok
    }

    fun lockApp() {
        if (_settings.value.isPinEnabled) {
            _isAppLocked.value = true
        }
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    suspend fun restoreBackupJson(json: String): Boolean {
        val ok = repository.restoreBackupJson(json)
        return ok
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            onComplete()
        }
    }
}
