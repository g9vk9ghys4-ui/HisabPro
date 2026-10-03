package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity
import com.example.ui.screens.MainAppScaffold
import com.example.ui.theme.HisabProTheme
import com.example.ui.viewmodel.HisabViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Seed realistic starter ledger data if database is brand new
        checkAndSeedInitialData()

        setContent {
            val viewModel: HisabViewModel = viewModel()
            HisabProTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndSeedInitialData() {
        val prefs = getSharedPreferences("hisabpro_prefs", Context.MODE_PRIVATE)
        val isSeeded = prefs.getBoolean("is_initial_seeded", false)
        if (!isSeeded) {
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val existing = db.partyDao().getAllParties().first()
                if (existing.isEmpty()) {
                    // Seed Rahul (Customer with AC Service ₹1,500 You'll Get)
                    val rahulId = db.partyDao().insertParty(
                        PartyEntity(
                            name = "Rahul Sharma",
                            phone = "9876543210",
                            address = "Model Town, Delhi",
                            businessName = "Rahul Tech Services",
                            notes = "AC Service & Repair customer",
                            isSupplier = false,
                            collectionDate = System.currentTimeMillis() + 86400000L // Tomorrow
                        )
                    )
                    db.transactionDao().insertTransaction(
                        TransactionEntity(
                            partyId = rahulId,
                            type = "GAVE", // You gave service on credit
                            amount = 1500.0,
                            description = "AC Service & Gas Refill",
                            paymentMethod = "Cash",
                            dateMillis = System.currentTimeMillis() - 3600000L
                        )
                    )

                    // Seed Priya Stores (Supplier)
                    val priyaId = db.partyDao().insertParty(
                        PartyEntity(
                            name = "Priya Hardware & Electricals",
                            phone = "9812345678",
                            address = "Market Yard, Shop #12",
                            businessName = "Priya Wholesale",
                            notes = "Spare parts supplier",
                            isSupplier = true
                        )
                    )
                    db.transactionDao().insertTransaction(
                        TransactionEntity(
                            partyId = priyaId,
                            type = "GOT", // Got goods on credit
                            amount = 3200.0,
                            description = "Copper pipes and capacitors",
                            paymentMethod = "Bank",
                            dateMillis = System.currentTimeMillis() - 7200000L
                        )
                    )
                    db.transactionDao().insertTransaction(
                        TransactionEntity(
                            partyId = priyaId,
                            type = "GAVE", // Paid partial
                            amount = 1000.0,
                            description = "Part payment via UPI",
                            paymentMethod = "UPI",
                            dateMillis = System.currentTimeMillis() - 1800000L
                        )
                    )

                    // Seed Amit Verma (Settled Customer)
                    val amitId = db.partyDao().insertParty(
                        PartyEntity(
                            name = "Amit Verma",
                            phone = "9988776655",
                            address = "Sector 14",
                            businessName = "Verma Consultancy",
                            isSupplier = false
                        )
                    )
                    db.transactionDao().insertTransaction(
                        TransactionEntity(
                            partyId = amitId,
                            type = "GAVE",
                            amount = 800.0,
                            description = "Maintenance check",
                            paymentMethod = "Cash",
                            dateMillis = System.currentTimeMillis() - 86400000L
                        )
                    )
                    db.transactionDao().insertTransaction(
                        TransactionEntity(
                            partyId = amitId,
                            type = "GOT",
                            amount = 800.0,
                            description = "Full payment received",
                            paymentMethod = "UPI",
                            dateMillis = System.currentTimeMillis() - 43200000L
                        )
                    )
                }
                prefs.edit().putBoolean("is_initial_seeded", true).apply()
            }
        }
    }
}
