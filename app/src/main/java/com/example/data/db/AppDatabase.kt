package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CashbookDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.IncomeDao
import com.example.data.dao.PartyDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.CashbookEntryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity

@Database(
    entities = [
        PartyEntity::class,
        TransactionEntity::class,
        CashbookEntryEntity::class,
        ExpenseEntity::class,
        IncomeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partyDao(): PartyDao
    abstract fun transactionDao(): TransactionDao
    abstract fun cashbookDao(): CashbookDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisabpro_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
