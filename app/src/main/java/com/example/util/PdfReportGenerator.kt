package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    data class PdfReportData(
        val cafeName: String,
        val cafeAddress: String,
        val cafePhone: String,
        val startDateFormatted: String,
        val endDateFormatted: String,
        val transactions: List<TransactionEntity>,
        val items: List<TransactionItemEntity>,
        val totalRevenue: Double,
        val totalNetProfit: Double,
        val cashTotal: Double,
        val qrisTotal: Double,
        val transferTotal: Double
    )

    fun generateSalesPdf(context: Context, data: PdfReportData): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

        // --- Colors ---
        val colorPrimary = Color.rgb(255, 107, 0) // Bright Orange
        val colorDark = Color.rgb(33, 33, 33)
        val colorGray = Color.rgb(117, 117, 117)
        val colorLightGray = Color.rgb(245, 245, 245)
        val colorBorder = Color.rgb(224, 224, 224)
        val colorGreen = Color.rgb(46, 125, 50)

        // 1. Header Banner
        paint.color = colorPrimary
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText(data.cafeName.ifBlank { "RENCANA KOPI" }, 30f, 40f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(data.cafeAddress.ifBlank { "Sistem Kasir & Manajemen Kafe" }, 30f, 58f, paint)
        canvas.drawText("Telp: ${data.cafePhone.ifBlank { "-" }}  •  Dicetak: ${dateFormat.format(Date())}", 30f, 74f, paint)

        var yPos = 120f

        // 2. Report Title & Period
        paint.color = colorDark
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("LAPORAN REKAP PENJUALAN KAFE", 30f, yPos, paint)

        yPos += 18f
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = colorGray
        canvas.drawText("Periode: ${data.startDateFormatted} s/d ${data.endDateFormatted}", 30f, yPos, paint)

        yPos += 22f

        // 3. Metric KPI Cards (3 boxes)
        val boxWidth = (pageWidth - 60 - 20) / 3f
        val boxHeight = 58f

        // Box 1: Total Orders
        drawKpiBox(canvas, paint, 30f, yPos, boxWidth, boxHeight, "TOTAL TRANSAKSI", "${data.transactions.size} Order", colorPrimary, colorLightGray, colorBorder)
        // Box 2: Total Omset (Gross Sales)
        drawKpiBox(canvas, paint, 30f + boxWidth + 10f, yPos, boxWidth, boxHeight, "TOTAL PENDAPATAN", rupiahFormat.format(data.totalRevenue), colorGreen, colorLightGray, colorBorder)
        // Box 3: Estimasi Laba Bersih
        drawKpiBox(canvas, paint, 30f + (boxWidth + 10f) * 2, yPos, boxWidth, boxHeight, "ESTIMASI LABA BERSIH", rupiahFormat.format(data.totalNetProfit), colorDark, colorLightGray, colorBorder)

        yPos += boxHeight + 25f

        // 4. Breakdown by Payment Method Table
        paint.color = colorDark
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Rincian Metode Pembayaran:", 30f, yPos, paint)

        yPos += 14f
        // Payment row
        paint.color = colorLightGray
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 24f, paint)

        paint.color = colorDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val cashTrx = data.transactions.count { it.paymentMethod.equals("CASH", true) }
        val qrisTrx = data.transactions.count { it.paymentMethod.equals("QRIS", true) }
        val transferTrx = data.transactions.count { it.paymentMethod.equals("TRANSFER", true) }

        canvas.drawText("Tunai (Cash): ${rupiahFormat.format(data.cashTotal)} ($cashTrx trx)", 40f, yPos + 16f, paint)
        canvas.drawText("QRIS: ${rupiahFormat.format(data.qrisTotal)} ($qrisTrx trx)", 230f, yPos + 16f, paint)
        canvas.drawText("Transfer Bank: ${rupiahFormat.format(data.transferTotal)} ($transferTrx trx)", 390f, yPos + 16f, paint)

        yPos += 40f

        // 5. Item Sales Table Header
        paint.color = colorDark
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Rincian Produk Terjual (Item Sales Breakdown):", 30f, yPos, paint)

        yPos += 14f

        // Table Header Bar
        paint.color = Color.rgb(240, 240, 240)
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 20f, paint)

        paint.color = colorDark
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NO", 36f, yPos + 14f, paint)
        canvas.drawText("NAMA MENU / PRODUK", 70f, yPos + 14f, paint)
        canvas.drawText("QTY TERJUAL", 360f, yPos + 14f, paint)
        canvas.drawText("TOTAL PENJUALAN", 450f, yPos + 14f, paint)

        yPos += 22f

        // Aggregate items
        val itemGroup = mutableMapOf<String, Pair<Int, Double>>()
        data.items.forEach { item ->
            val curr = itemGroup[item.menuName] ?: Pair(0, 0.0)
            itemGroup[item.menuName] = Pair(curr.first + item.quantity, curr.second + item.subtotal)
        }
        val sortedItems = itemGroup.toList().sortedByDescending { it.second.first }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var index = 1
        val maxItemsToShow = 18 // Fit neatly on single page or max height

        for ((name, pair) in sortedItems.take(maxItemsToShow)) {
            val (qty, total) = pair
            // Zebra row background
            if (index % 2 == 0) {
                paint.color = Color.rgb(250, 250, 250)
                paint.style = Paint.Style.FILL
                canvas.drawRect(30f, yPos - 2f, (pageWidth - 30).toFloat(), yPos + 16f, paint)
            }

            paint.color = colorDark
            paint.textSize = 9f
            canvas.drawText("$index", 38f, yPos + 11f, paint)
            canvas.drawText(name.take(38), 70f, yPos + 11f, paint)
            canvas.drawText("$qty Cup / Item", 360f, yPos + 11f, paint)
            canvas.drawText(rupiahFormat.format(total), 450f, yPos + 11f, paint)

            yPos += 18f
            index++
        }

        if (sortedItems.size > maxItemsToShow) {
            paint.color = colorGray
            paint.textSize = 8.5f
            canvas.drawText("... dan ${sortedItems.size - maxItemsToShow} item lainnya", 70f, yPos + 10f, paint)
            yPos += 18f
        }

        // 6. Signatures & Footer
        val footerY = (pageHeight - 90).toFloat()
        paint.color = colorBorder
        paint.strokeWidth = 1f
        canvas.drawLine(30f, footerY, (pageWidth - 30).toFloat(), footerY, paint)

        paint.color = colorGray
        paint.textSize = 8.5f
        canvas.drawText("Laporan ini di-generate secara otomatis oleh Sistem Kasir Rencana Kopi Cloud POS.", 30f, footerY + 18f, paint)
        canvas.drawText("Tanda Tangan Penanggung Jawab:", 400f, footerY + 18f, paint)

        canvas.drawLine(400f, footerY + 60f, 540f, footerY + 60f, paint)
        canvas.drawText("( Owner / Kasir Bertugas )", 408f, footerY + 72f, paint)

        pdfDocument.finishPage(page)

        // Save to file
        return try {
            val reportDir = File(context.cacheDir, "reports")
            if (!reportDir.exists()) reportDir.mkdirs()
            val fileName = "Rekap_Penjualan_${System.currentTimeMillis()}.pdf"
            val file = File(reportDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawKpiBox(
        canvas: android.graphics.Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        valueColor: Int,
        bgColor: Int,
        borderColor: Int
    ) {
        // Background
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, paint)

        // Border
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, paint)

        // Title
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 10f, y + 18f, paint)

        // Value
        paint.color = valueColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 10f, y + 42f, paint)
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Laporan PDF dengan:"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi pembuka PDF ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Rekap Penjualan Kafe")
                putExtra(Intent.EXTRA_TEXT, "Berikut terlampir file PDF Laporan Rekap Penjualan Rencana Kopi.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Bagikan Laporan PDF via:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
