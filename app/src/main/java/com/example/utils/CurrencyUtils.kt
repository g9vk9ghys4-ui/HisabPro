package com.example.utils

import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

enum class BalanceStatus {
    YOU_WILL_GET,
    YOU_WILL_GIVE,
    SETTLED
}

data class PartyBalance(
    val netAmount: Double,
    val totalGave: Double,
    val totalGot: Double,
    val status: BalanceStatus
)

object CurrencyUtils {
    private val indianFormat = DecimalFormat("#,##,###.##")

    fun format(amount: Double, currency: String = "₹"): String {
        val positive = abs(amount)
        val formatted = if (positive % 1.0 == 0.0) {
            val longVal = positive.toLong()
            formatIndianNumber(longVal)
        } else {
            indianFormat.format(positive)
        }
        return "$currency$formatted"
    }

    private fun formatIndianNumber(n: Long): String {
        if (n < 1000) return n.toString()
        val s = n.toString()
        val lastThree = s.substring(s.length - 3)
        var rest = s.substring(0, s.length - 3)
        val result = StringBuilder()
        while (rest.length > 2) {
            result.insert(0, "," + rest.substring(rest.length - 2))
            rest = rest.substring(0, rest.length - 2)
        }
        result.insert(0, rest)
        return "$result,$lastThree"
    }

    /**
     * For Customers:
     * Gave (You gave goods/credit) -> increases what they owe -> You'll Get
     * Got (They paid) -> decreases what they owe
     * Net = totalGave - totalGot
     * If net > 0 -> You'll Get
     * If net < 0 -> You'll Give (customer gave advance)
     * If net == 0 -> Settled
     *
     * For Suppliers:
     * Got (Goods purchased on credit from supplier) -> increases what you owe supplier -> You'll Give
     * Gave (You paid supplier) -> decreases what you owe supplier
     * Net = totalGot - totalGave
     * If net > 0 -> You'll Give
     * If net < 0 -> You'll Get (advance given to supplier)
     * If net == 0 -> Settled
     */
    fun calculateBalance(party: PartyEntity, transactions: List<TransactionEntity>): PartyBalance {
        var totalGave = 0.0
        var totalGot = 0.0

        for (tx in transactions) {
            if (tx.partyId == party.id) {
                if (tx.type == "GAVE") {
                    totalGave += tx.amount
                } else if (tx.type == "GOT") {
                    totalGot += tx.amount
                }
            }
        }

        return if (!party.isSupplier) {
            // Customer
            val diff = totalGave - totalGot
            when {
                diff > 0.01 -> PartyBalance(diff, totalGave, totalGot, BalanceStatus.YOU_WILL_GET)
                diff < -0.01 -> PartyBalance(abs(diff), totalGave, totalGot, BalanceStatus.YOU_WILL_GIVE)
                else -> PartyBalance(0.0, totalGave, totalGot, BalanceStatus.SETTLED)
            }
        } else {
            // Supplier
            val diff = totalGot - totalGave
            when {
                diff > 0.01 -> PartyBalance(diff, totalGave, totalGot, BalanceStatus.YOU_WILL_GIVE)
                diff < -0.01 -> PartyBalance(abs(diff), totalGave, totalGot, BalanceStatus.YOU_WILL_GET)
                else -> PartyBalance(0.0, totalGave, totalGot, BalanceStatus.SETTLED)
            }
        }
    }
}
