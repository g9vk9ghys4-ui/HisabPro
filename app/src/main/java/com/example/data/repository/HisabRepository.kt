package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.entity.CashbookEntryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class HisabRepository(private val database: AppDatabase) {
    private val partyDao = database.partyDao()
    private val transactionDao = database.transactionDao()
    private val cashbookDao = database.cashbookDao()
    private val expenseDao = database.expenseDao()
    private val incomeDao = database.incomeDao()

    fun getParties(isSupplier: Boolean): Flow<List<PartyEntity>> = partyDao.getParties(isSupplier)
    fun getAllParties(): Flow<List<PartyEntity>> = partyDao.getAllParties()
    fun getParty(id: Long): Flow<PartyEntity?> = partyDao.getPartyById(id)
    suspend fun getPartySync(id: Long): PartyEntity? = partyDao.getPartyByIdSync(id)

    suspend fun saveParty(party: PartyEntity): Long = withContext(Dispatchers.IO) {
        if (party.id == 0L) {
            partyDao.insertParty(party)
        } else {
            partyDao.updateParty(party)
            party.id
        }
    }

    suspend fun deleteParty(party: PartyEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionsForParty(party.id)
        cashbookDao.deleteEntriesByKeyword(party.name)
        partyDao.deleteParty(party)
    }

    suspend fun updateCollectionDate(partyId: Long, date: Long?) = withContext(Dispatchers.IO) {
        partyDao.updateCollectionDate(partyId, date)
    }

    // Transactions
    fun getTransactionsForParty(partyId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForParty(partyId)

    fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    suspend fun addTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val id = transactionDao.insertTransaction(transaction)
        // Auto-reflect in cashbook if payment method is Cash
        if (transaction.paymentMethod.equals("Cash", ignoreCase = true)) {
            val cashType = if (transaction.type == "GOT") "IN" else "OUT"
            val party = partyDao.getPartyByIdSync(transaction.partyId)
            val desc = "${if (transaction.type == "GOT") "Received from" else "Given to"} ${party?.name ?: "Customer"}${if (transaction.description.isNotBlank()) " - " + transaction.description else ""}"
            cashbookDao.insertEntry(
                CashbookEntryEntity(
                    type = cashType,
                    amount = transaction.amount,
                    description = desc,
                    paymentMethod = "Cash",
                    dateMillis = transaction.dateMillis
                )
            )
        }
        id
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }

    // Cashbook
    fun getAllCashbookEntries(): Flow<List<CashbookEntryEntity>> = cashbookDao.getAllEntries()

    suspend fun addCashbookEntry(entry: CashbookEntryEntity): Long = withContext(Dispatchers.IO) {
        cashbookDao.insertEntry(entry)
    }

    suspend fun deleteCashbookEntry(entry: CashbookEntryEntity) = withContext(Dispatchers.IO) {
        cashbookDao.deleteEntry(entry)
    }

    // Expenses
    fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun addExpense(expense: ExpenseEntity): Long = withContext(Dispatchers.IO) {
        val id = expenseDao.insertExpense(expense)
        if (expense.paymentMethod.equals("Cash", ignoreCase = true)) {
            cashbookDao.insertEntry(
                CashbookEntryEntity(
                    type = "OUT",
                    amount = expense.amount,
                    description = "Expense: [${expense.category}] ${expense.description}",
                    paymentMethod = "Cash",
                    dateMillis = expense.dateMillis
                )
            )
        }
        id
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    // Incomes
    fun getAllIncomes(): Flow<List<IncomeEntity>> = incomeDao.getAllIncomes()

    suspend fun addIncome(income: IncomeEntity): Long = withContext(Dispatchers.IO) {
        val id = incomeDao.insertIncome(income)
        if (income.paymentMethod.equals("Cash", ignoreCase = true)) {
            cashbookDao.insertEntry(
                CashbookEntryEntity(
                    type = "IN",
                    amount = income.amount,
                    description = "Income: [${income.category}] ${income.description}",
                    paymentMethod = "Cash",
                    dateMillis = income.dateMillis
                )
            )
        }
        id
    }

    suspend fun deleteIncome(income: IncomeEntity) = withContext(Dispatchers.IO) {
        incomeDao.deleteIncome(income)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        partyDao.clearAll()
        transactionDao.clearAll()
        cashbookDao.clearAll()
        expenseDao.clearAll()
        incomeDao.clearAll()
    }

    // Backup Export to JSON string
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "HisabPro")
        root.put("exportTimestamp", System.currentTimeMillis())

        val parties = partyDao.getAllParties().first()
        val partiesArr = JSONArray()
        parties.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("phone", p.phone)
            obj.put("address", p.address)
            obj.put("businessName", p.businessName)
            obj.put("notes", p.notes)
            obj.put("isSupplier", p.isSupplier)
            if (p.collectionDate != null) obj.put("collectionDate", p.collectionDate)
            obj.put("createdAt", p.createdAt)
            partiesArr.put(obj)
        }
        root.put("parties", partiesArr)

        val txs = transactionDao.getAllTransactions().first()
        val txArr = JSONArray()
        txs.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("partyId", t.partyId)
            obj.put("type", t.type)
            obj.put("amount", t.amount)
            obj.put("description", t.description)
            obj.put("paymentMethod", t.paymentMethod)
            obj.put("dateMillis", t.dateMillis)
            obj.put("createdAt", t.createdAt)
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        val cash = cashbookDao.getAllEntries().first()
        val cashArr = JSONArray()
        cash.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("type", c.type)
            obj.put("amount", c.amount)
            obj.put("description", c.description)
            obj.put("paymentMethod", c.paymentMethod)
            obj.put("dateMillis", c.dateMillis)
            cashArr.put(obj)
        }
        root.put("cashbook", cashArr)

        val expenses = expenseDao.getAllExpenses().first()
        val expArr = JSONArray()
        expenses.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("category", e.category)
            obj.put("amount", e.amount)
            obj.put("description", e.description)
            obj.put("paymentMethod", e.paymentMethod)
            obj.put("dateMillis", e.dateMillis)
            expArr.put(obj)
        }
        root.put("expenses", expArr)

        val incomes = incomeDao.getAllIncomes().first()
        val incArr = JSONArray()
        incomes.forEach { i ->
            val obj = JSONObject()
            obj.put("id", i.id)
            obj.put("category", i.category)
            obj.put("amount", i.amount)
            obj.put("description", i.description)
            obj.put("paymentMethod", i.paymentMethod)
            obj.put("dateMillis", i.dateMillis)
            incArr.put(obj)
        }
        root.put("incomes", incArr)

        root.toString(2)
    }

    // Restore Backup from JSON string
    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            clearAllData()

            if (root.has("parties")) {
                val partiesArr = root.getJSONArray("parties")
                for (i in 0 until partiesArr.length()) {
                    val obj = partiesArr.getJSONObject(i)
                    partyDao.insertParty(
                        PartyEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            businessName = obj.optString("businessName", ""),
                            notes = obj.optString("notes", ""),
                            isSupplier = obj.optBoolean("isSupplier", false),
                            collectionDate = if (obj.has("collectionDate")) obj.getLong("collectionDate") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("transactions")) {
                val txArr = root.getJSONArray("transactions")
                for (i in 0 until txArr.length()) {
                    val obj = txArr.getJSONObject(i)
                    transactionDao.insertTransaction(
                        TransactionEntity(
                            id = obj.optLong("id", 0L),
                            partyId = obj.getLong("partyId"),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            description = obj.optString("description", ""),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("cashbook")) {
                val cashArr = root.getJSONArray("cashbook")
                for (i in 0 until cashArr.length()) {
                    val obj = cashArr.getJSONObject(i)
                    cashbookDao.insertEntry(
                        CashbookEntryEntity(
                            id = obj.optLong("id", 0L),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            description = obj.optString("description", ""),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("expenses")) {
                val expArr = root.getJSONArray("expenses")
                for (i in 0 until expArr.length()) {
                    val obj = expArr.getJSONObject(i)
                    expenseDao.insertExpense(
                        ExpenseEntity(
                            id = obj.optLong("id", 0L),
                            category = obj.getString("category"),
                            amount = obj.getDouble("amount"),
                            description = obj.optString("description", ""),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("incomes")) {
                val incArr = root.getJSONArray("incomes")
                for (i in 0 until incArr.length()) {
                    val obj = incArr.getJSONObject(i)
                    incomeDao.insertIncome(
                        IncomeEntity(
                            id = obj.optLong("id", 0L),
                            category = obj.getString("category"),
                            amount = obj.getDouble("amount"),
                            description = obj.optString("description", ""),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
