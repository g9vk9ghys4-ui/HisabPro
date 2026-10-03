package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parties",
    indices = [Index(value = ["phone"]), Index(value = ["isSupplier"])]
)
data class PartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val businessName: String = "",
    val notes: String = "",
    val isSupplier: Boolean = false, // false = Customer, true = Supplier
    val collectionDate: Long? = null, // epoch millis for reminder
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["partyId"]), Index(value = ["dateMillis"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partyId: Long,
    val type: String, // "GAVE" (You gave money) or "GOT" (You got money)
    val amount: Double,
    val description: String = "",
    val paymentMethod: String = "Cash", // Cash, UPI, Bank, Other
    val dateMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashbook")
data class CashbookEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "IN" or "OUT"
    val amount: Double,
    val description: String = "",
    val paymentMethod: String = "Cash",
    val dateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // Shop, Transport, Food, Electricity, Rent, Salary, Stock, Repair, Other
    val amount: Double,
    val description: String = "",
    val paymentMethod: String = "Cash",
    val dateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // Sales, Service, Commission, Other
    val amount: Double,
    val description: String = "",
    val paymentMethod: String = "Cash",
    val dateMillis: Long = System.currentTimeMillis()
)
