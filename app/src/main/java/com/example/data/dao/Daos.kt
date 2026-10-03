package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CashbookEntryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties WHERE isSupplier = :isSupplier ORDER BY name ASC")
    fun getParties(isSupplier: Boolean): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties ORDER BY name ASC")
    fun getAllParties(): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    fun getPartyById(id: Long): Flow<PartyEntity?>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    suspend fun getPartyByIdSync(id: Long): PartyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyEntity): Long

    @Update
    suspend fun updateParty(party: PartyEntity)

    @Delete
    suspend fun deleteParty(party: PartyEntity)

    @Query("UPDATE parties SET collectionDate = :date WHERE id = :partyId")
    suspend fun updateCollectionDate(partyId: Long, date: Long?)

    @Query("DELETE FROM parties")
    suspend fun clearAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE partyId = :partyId ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsForParty(partyId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE partyId = :partyId")
    suspend fun deleteTransactionsForParty(partyId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface CashbookDao {
    @Query("SELECT * FROM cashbook ORDER BY dateMillis DESC, id DESC")
    fun getAllEntries(): Flow<List<CashbookEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CashbookEntryEntity): Long

    @Delete
    suspend fun deleteEntry(entry: CashbookEntryEntity)

    @Query("DELETE FROM cashbook WHERE description LIKE '%' || :keyword || '%'")
    suspend fun deleteEntriesByKeyword(keyword: String)

    @Query("DELETE FROM cashbook")
    suspend fun clearAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes ORDER BY dateMillis DESC, id DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes")
    suspend fun clearAll()
}
