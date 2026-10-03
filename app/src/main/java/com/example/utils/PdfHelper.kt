package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream

object PdfHelper {

    fun generateAndShareStatement(
        context: Context,
        businessName: String,
        businessPhone: String,
        party: PartyEntity,
        transactions: List<TransactionEntity>,
        balance: PartyBalance
    ) {
        try {
            val doc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Paints
            val primaryPaint = Paint().apply {
                color = Color.rgb(30, 64, 175) // Hisab Blue
                style = Paint.Style.FILL
            }
            val titlePaint = Paint().apply {
                color = Color.rgb(30, 64, 175)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val taglinePaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 10f
                isAntiAlias = true
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 10f
                isAntiAlias = true
            }
            val boldBodyPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }
            val greenPaint = Paint().apply {
                color = Color.rgb(22, 163, 74)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val redPaint = Paint().apply {
                color = Color.rgb(220, 38, 38)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            // Top Header Bar
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 8f, primaryPaint)

            var y = 45f
            // Brand & Title
            canvas.drawText("HisabPro", 40f, y, titlePaint)
            canvas.drawText("Simple business. Clear hisab.", 40f, y + 14f, taglinePaint)

            // Business info on right
            val bizPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            val bizSubPaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            canvas.drawText(businessName, pageWidth - 40f, y - 5f, bizPaint)
            if (businessPhone.isNotBlank()) {
                canvas.drawText("Phone: $businessPhone", pageWidth - 40f, y + 10f, bizSubPaint)
            }
            canvas.drawText("Date: ${DateUtils.formatDate(System.currentTimeMillis())}", pageWidth - 40f, y + 24f, bizSubPaint)

            y += 45f
            canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)

            // Statement Details Box
            y += 20f
            val cardRect = RectF(40f, y, pageWidth - 40f, y + 65f)
            val cardBgPaint = Paint().apply {
                color = Color.rgb(248, 250, 252)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)

            canvas.drawText("ACCOUNT STATEMENT", 55f, y + 22f, headerPaint)
            canvas.drawText("Party Name: ${party.name}", 55f, y + 40f, boldBodyPaint)
            if (party.phone.isNotBlank()) {
                canvas.drawText("Contact: ${party.phone}", 55f, y + 54f, bodyPaint)
            }

            // Balance Summary in the box (right aligned)
            val statusColor = when (balance.status) {
                BalanceStatus.YOU_WILL_GET -> greenPaint
                BalanceStatus.YOU_WILL_GIVE -> redPaint
                BalanceStatus.SETTLED -> boldBodyPaint
            }
            val statusLabel = when (balance.status) {
                BalanceStatus.YOU_WILL_GET -> "NET: YOU'LL GET"
                BalanceStatus.YOU_WILL_GIVE -> "NET: YOU'LL GIVE"
                BalanceStatus.SETTLED -> "NET: SETTLED"
            }
            val rightAlignPaint = Paint(statusColor).apply { textAlign = Paint.Align.RIGHT }
            val rightLabelPaint = Paint(boldBodyPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 9f }
            canvas.drawText(statusLabel, pageWidth - 55f, y + 26f, rightLabelPaint)
            canvas.drawText(CurrencyUtils.format(balance.netAmount), pageWidth - 55f, y + 48f, rightAlignPaint.apply { textSize = 16f })

            y += 85f

            // Table Header
            val tableHeadBg = Paint().apply { color = Color.rgb(241, 245, 249) }
            canvas.drawRect(40f, y, pageWidth - 40f, y + 24f, tableHeadBg)
            val thPaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("DATE & TIME", 50f, y + 16f, thPaint)
            canvas.drawText("DESCRIPTION / DETAILS", 160f, y + 16f, thPaint)
            canvas.drawText("MODE", 340f, y + 16f, thPaint)
            canvas.drawText("GAVE (DR)", 410f, y + 16f, thPaint)
            canvas.drawText("GOT (CR)", 480f, y + 16f, thPaint)

            y += 24f

            // Transactions rows
            // Sort ascending for ledger calculation
            val sortedTxs = transactions.sortedBy { it.dateMillis }
            var rowCount = 0

            for (tx in sortedTxs) {
                if (y > pageHeight - 90) break // Page bound
                rowCount++
                y += 20f

                canvas.drawText(DateUtils.formatDateTime(tx.dateMillis), 50f, y, bodyPaint)
                val desc = if (tx.description.isBlank()) "-" else tx.description.take(24)
                canvas.drawText(desc, 160f, y, bodyPaint)
                canvas.drawText(tx.paymentMethod, 340f, y, bodyPaint)

                if (tx.type == "GAVE") {
                    canvas.drawText(CurrencyUtils.format(tx.amount), 410f, y, redPaint.apply { textSize = 10f })
                    canvas.drawText("-", 495f, y, bodyPaint)
                } else {
                    canvas.drawText("-", 425f, y, bodyPaint)
                    canvas.drawText(CurrencyUtils.format(tx.amount), 480f, y, greenPaint.apply { textSize = 10f })
                }
                canvas.drawLine(40f, y + 6f, pageWidth - 40f, y + 6f, linePaint)
            }

            if (sortedTxs.isEmpty()) {
                y += 30f
                canvas.drawText("No transactions recorded yet.", 220f, y, bodyPaint)
            }

            // Totals Row
            y += 28f
            val totalsBox = RectF(40f, y, pageWidth - 40f, y + 36f)
            canvas.drawRect(totalsBox, cardBgPaint)
            canvas.drawText("TOTALS", 50f, y + 22f, boldBodyPaint)
            canvas.drawText("Total Gave: ${CurrencyUtils.format(balance.totalGave)}", 220f, y + 22f, redPaint.apply { textSize = 11f })
            canvas.drawText("Total Received: ${CurrencyUtils.format(balance.totalGot)}", 380f, y + 22f, greenPaint.apply { textSize = 11f })

            // Footer
            val footerY = pageHeight - 40f
            canvas.drawLine(40f, footerY - 10f, pageWidth - 40f, footerY - 10f, linePaint)
            val footerPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 9f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Generated by HisabPro App • Thank you for your business!", pageWidth / 2f, footerY + 8f, footerPaint)

            doc.finishPage(page)

            // Save PDF to cache
            val dir = File(context.cacheDir, "statements")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "Statement_${party.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            doc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            doc.close()

            // Share via FileProvider
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Account Statement - ${party.name}")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the account statement for ${party.name} from $businessName.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Statement PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to create PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
