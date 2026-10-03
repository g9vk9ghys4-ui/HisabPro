package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity
import java.net.URLEncoder

object ShareHelper {

    fun openDialer(context: Context, phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phone.trim()}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        try {
            var cleanPhone = phone.replace(Regex("[^0-9+]"), "").trim()
            if (!cleanPhone.startsWith("+") && cleanPhone.length == 10) {
                cleanPhone = "91$cleanPhone" // Standard Indian prefix default
            } else if (cleanPhone.startsWith("+")) {
                cleanPhone = cleanPhone.substring(1)
            }

            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share
            shareText(context, "Hisab Reminder", message)
        }
    }

    fun shareText(context: Context, subject: String, text: String) {
        try {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share via")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildReminderMessage(partyName: String, amountFormatted: String, businessName: String): String {
        return "Hello $partyName,\n\n" +
                "This is a friendly reminder regarding your pending amount of $amountFormatted.\n" +
                "Please make the payment when convenient.\n\n" +
                "Thank you,\n$businessName\n(Powered by HisabPro)"
    }

    fun buildReceiptMessage(
        partyName: String,
        amountFormatted: String,
        type: String,
        method: String,
        dateFormatted: String,
        remainingBalance: String,
        businessName: String
    ): String {
        val actionText = if (type == "GOT") "Payment Received" else "Payment Given"
        return "🧾 *PAYMENT RECEIPT*\n" +
                "------------------------\n" +
                "*$businessName*\n\n" +
                "Party: $partyName\n" +
                "Type: $actionText\n" +
                "Amount: $amountFormatted\n" +
                "Payment Method: $method\n" +
                "Date: $dateFormatted\n" +
                "Current Balance: $remainingBalance\n\n" +
                "Thank you for your business!\n_HisabPro - Simple business. Clear hisab._"
    }

    fun buildStatementSummary(
        party: PartyEntity,
        transactions: List<TransactionEntity>,
        balance: PartyBalance,
        businessName: String
    ): String {
        val sb = StringBuilder()
        sb.append("📋 *ACCOUNT STATEMENT*\n")
        sb.append("------------------------\n")
        sb.append("*$businessName*\n")
        sb.append("Party: ${party.name}\n")
        if (party.phone.isNotBlank()) sb.append("Phone: ${party.phone}\n")
        sb.append("Generated: ${DateUtils.formatDate(System.currentTimeMillis())}\n")
        sb.append("------------------------\n\n")

        sb.append("Total Given: ${CurrencyUtils.format(balance.totalGave)}\n")
        sb.append("Total Received: ${CurrencyUtils.format(balance.totalGot)}\n")
        val statusText = when (balance.status) {
            BalanceStatus.YOU_WILL_GET -> "Pending (You'll Get): ${CurrencyUtils.format(balance.netAmount)}"
            BalanceStatus.YOU_WILL_GIVE -> "Advance (You'll Give): ${CurrencyUtils.format(balance.netAmount)}"
            BalanceStatus.SETTLED -> "Balance: Settled (₹0)"
        }
        sb.append("*$statusText*\n\n")
        sb.append("Recent Transactions:\n")

        transactions.take(15).forEach { tx ->
            val typeStr = if (tx.type == "GAVE") "[-] GAVE" else "[+] GOT"
            sb.append("${DateUtils.formatShortDate(tx.dateMillis)} | $typeStr ${CurrencyUtils.format(tx.amount)} (${tx.paymentMethod})\n")
            if (tx.description.isNotBlank()) {
                sb.append("   Note: ${tx.description}\n")
            }
        }

        sb.append("\n_Thank you for your business!_\n_HisabPro_")
        return sb.toString()
    }

    fun exportPartiesCsv(parties: List<PartyEntity>, transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,Name,Phone,Type,Total Gave,Total Received,Balance Amount,Status,Notes\n")
        parties.forEach { p ->
            val balance = CurrencyUtils.calculateBalance(p, transactions)
            val typeStr = if (p.isSupplier) "Supplier" else "Customer"
            val statusStr = when (balance.status) {
                BalanceStatus.YOU_WILL_GET -> "You'll Get"
                BalanceStatus.YOU_WILL_GIVE -> "You'll Give"
                BalanceStatus.SETTLED -> "Settled"
            }
            sb.append("${p.id},\"${p.name}\",\"${p.phone}\",\"$typeStr\",${balance.totalGave},${balance.totalGot},${balance.netAmount},\"$statusStr\",\"${p.notes.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    fun exportTransactionsCsv(transactions: List<TransactionEntity>, partiesMap: Map<Long, PartyEntity>): String {
        val sb = StringBuilder()
        sb.append("Transaction ID,Date,Party Name,Phone,Type,Amount,Payment Method,Description\n")
        transactions.forEach { tx ->
            val party = partiesMap[tx.partyId]
            val dateStr = DateUtils.formatDateTime(tx.dateMillis)
            val typeStr = if (tx.type == "GAVE") "Money Given" else "Money Received"
            sb.append("${tx.id},\"$dateStr\",\"${party?.name ?: "Unknown"}\",\"${party?.phone ?: ""}\",\"$typeStr\",${tx.amount},\"${tx.paymentMethod}\",\"${tx.description.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }
}
