package com.example.ui.screens.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity
import com.example.ui.theme.HisabBlue
import com.example.ui.theme.HisabGreen
import com.example.ui.theme.HisabGreenContainer
import com.example.ui.theme.HisabGreenOnContainer
import com.example.ui.theme.HisabRed
import com.example.ui.theme.HisabRedOnContainer
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.ShareHelper

@Composable
fun PaymentReceiptDialog(
    transaction: TransactionEntity,
    party: PartyEntity,
    remainingBalanceStr: String,
    businessName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isGot = transaction.type == "GOT"
    val formattedAmount = CurrencyUtils.format(transaction.amount)
    val formattedDate = DateUtils.formatDateTime(transaction.dateMillis)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(HisabGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = HisabGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Payment Recorded!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isGot) "Money Received" else "Money Given",
                    fontSize = 13.sp,
                    color = if (isGot) HisabGreen else HisabRed,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReceiptRow(label = "Party", value = party.name, isBold = true)
                        ReceiptRow(label = "Amount", value = formattedAmount, isBold = true, valueColor = if (isGot) HisabGreen else HisabRed)
                        ReceiptRow(label = "Payment Mode", value = transaction.paymentMethod)
                        ReceiptRow(label = "Date & Time", value = formattedDate)
                        if (transaction.description.isNotBlank()) {
                            ReceiptRow(label = "Note", value = transaction.description)
                        }
                        ReceiptRow(label = "Updated Balance", value = remainingBalanceStr, isBold = true)
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // WhatsApp share button
                OutlinedButton(
                    onClick = {
                        val message = ShareHelper.buildReceiptMessage(
                            partyName = party.name,
                            amountFormatted = formattedAmount,
                            type = transaction.type,
                            method = transaction.paymentMethod,
                            dateFormatted = formattedDate,
                            remainingBalance = remainingBalanceStr,
                            businessName = businessName
                        )
                        ShareHelper.openWhatsApp(context, party.phone, message)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("whatsapp_receipt_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Done button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HisabBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("receipt_done_button")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = null
    )
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}
