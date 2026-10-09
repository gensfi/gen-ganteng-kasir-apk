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
import com.example.data.local.entity.IngredientPurchaseEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.repository.CashierSalesSummary
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
        val onlineTotal: Double = 0.0,
        val transferTotal: Double = onlineTotal,
        val purchases: List<IngredientPurchaseEntity> = emptyList(),
        val totalPurchaseCost: Double = 0.0,
        val cashierSales: List<CashierSalesSummary> = emptyList()
    )

    data class PdfPurchaseReportData(
        val cafeName: String,
        val cafeAddress: String,
        val cafePhone: String,
        val printedDateFormatted: String,
        val purchases: List<IngredientPurchaseEntity>,
        val totalCost: Double
    )

    fun generateSalesPdf(context: Context, data: PdfReportData): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val dateTimeFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale("id", "ID"))
        val fullDateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

        // --- Colors ---
        val colorPrimary = Color.rgb(255, 107, 0) // Orange Brand
        val colorDark = Color.rgb(33, 33, 33)
        val colorGray = Color.rgb(117, 117, 117)
        val colorLightGray = Color.rgb(245, 245, 245)
        val colorBorder = Color.rgb(224, 224, 224)
        val colorGreen = Color.rgb(46, 125, 50)
        val colorRed = Color.rgb(198, 40, 40)
        val colorBlue = Color.rgb(25, 118, 210)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader(title: String, subtitle: String) {
            paint.color = colorPrimary
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 75f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            canvas.drawText(data.cafeName.ifBlank { "RENCANA KOPI" }, 30f, 32f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(data.cafeAddress.ifBlank { "Sistem Kasir & Manajemen Kafe" }, 30f, 48f, paint)
            canvas.drawText("Telp: ${data.cafePhone.ifBlank { "-" }}  •  Dicetak: ${fullDateFormat.format(Date())}", 30f, 62f, paint)

            paint.color = colorDark
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(title, 30f, 98f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = colorGray
            canvas.drawText(subtitle, 30f, 112f, paint)
        }

        fun checkPageBreak(requiredHeight: Float, currentY: Float): Float {
            if (currentY + requiredHeight > pageHeight - 65f) {
                // Draw page number on current page
                paint.color = colorGray
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Halaman $pageNumber", (pageWidth - 80).toFloat(), (pageHeight - 20).toFloat(), paint)

                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // Small banner for subsequent pages
                paint.color = colorPrimary
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 30f, paint)
                paint.color = Color.WHITE
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${data.cafeName.ifBlank { "RENCANA KOPI" }} - Laporan Penjualan (Hal $pageNumber)", 30f, 20f, paint)

                return 50f
            }
            return currentY
        }

        // Draw Page 1
        drawHeader("LAPORAN REKAP PENJUALAN & OMSET KAFE", "Periode: ${data.startDateFormatted} s/d ${data.endDateFormatted}")
        var yPos = 130f

        // 1. KPI Cards Row (4 boxes)
        val boxWidth = (pageWidth - 60 - 30) / 4f
        val boxHeight = 52f

        drawKpiBox(canvas, paint, 30f, yPos, boxWidth, boxHeight, "TOTAL TRANSAKSI", "${data.transactions.size} Order", colorPrimary, colorLightGray, colorBorder)
        drawKpiBox(canvas, paint, 30f + (boxWidth + 10f), yPos, boxWidth, boxHeight, "TOTAL OMSET", rupiahFormat.format(data.totalRevenue), colorGreen, colorLightGray, colorBorder)
        drawKpiBox(canvas, paint, 30f + (boxWidth + 10f) * 2, yPos, boxWidth, boxHeight, "BELANJA BAHAN", rupiahFormat.format(data.totalPurchaseCost), colorRed, colorLightGray, colorBorder)
        drawKpiBox(canvas, paint, 30f + (boxWidth + 10f) * 3, yPos, boxWidth, boxHeight, "LABA BERSIH", rupiahFormat.format(data.totalNetProfit), colorDark, colorLightGray, colorBorder)

        yPos += boxHeight + 20f

        // 2. Payment Method Summary
        paint.color = colorDark
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Rincian Metode Pembayaran:", 30f, yPos, paint)
        yPos += 14f

        paint.color = colorLightGray
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 22f, paint)

        val cashTrx = data.transactions.count { it.paymentMethod.equals("CASH", true) }
        val qrisTrx = data.transactions.count { it.paymentMethod.equals("QRIS", true) }
        val onlineTrx = data.transactions.count { it.paymentMethod.equals("ONLINE", true) || it.paymentMethod.equals("PESANAN_ONLINE", true) || it.paymentMethod.equals("TRANSFER", true) }
        val effectiveOnlineTotal = if (data.onlineTotal > 0) data.onlineTotal else data.transferTotal

        paint.color = colorDark
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Tunai: ${rupiahFormat.format(data.cashTotal)} ($cashTrx trx)", 38f, yPos + 15f, paint)
        canvas.drawText("QRIS: ${rupiahFormat.format(data.qrisTotal)} ($qrisTrx trx)", 215f, yPos + 15f, paint)
        canvas.drawText("Online/Transfer: ${rupiahFormat.format(effectiveOnlineTotal)} ($onlineTrx trx)", 375f, yPos + 15f, paint)

        yPos += 34f

        // 3. Ringkasan Omset per Kasir yang Menjual (Cashier Performance)
        yPos = checkPageBreak(100f, yPos)
        paint.color = colorDark
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("👤 Ringkasan Omset per Kasir yang Menjual:", 30f, yPos, paint)
        yPos += 14f

        // Cashier Table Header
        paint.color = Color.rgb(235, 235, 235)
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 18f, paint)

        paint.color = colorDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NO", 36f, yPos + 13f, paint)
        canvas.drawText("NAMA KASIR BERTUGAS", 65f, yPos + 13f, paint)
        canvas.drawText("JML TRANSAKSI", 250f, yPos + 13f, paint)
        canvas.drawText("ITEM TERJUAL", 360f, yPos + 13f, paint)
        canvas.drawText("TOTAL OMSET KASIR", 455f, yPos + 13f, paint)
        yPos += 20f

        // Group by cashier
        val cashierGroups = data.transactions.groupBy { it.cashierName.ifBlank { "Kasir" } }
        val trxIdToCashier = data.transactions.associate { it.id to it.cashierName.ifBlank { "Kasir" } }

        var cIdx = 1
        cashierGroups.forEach { (cName, trxs) ->
            yPos = checkPageBreak(20f, yPos)
            if (cIdx % 2 == 0) {
                paint.color = Color.rgb(250, 250, 250)
                paint.style = Paint.Style.FILL
                canvas.drawRect(30f, yPos - 2f, (pageWidth - 30).toFloat(), yPos + 15f, paint)
            }

            val tIds = trxs.map { it.id }.toSet()
            val totalSoldQty = data.items.filter { it.transactionId in tIds }.sumOf { it.quantity }
            val cRevenue = trxs.sumOf { it.totalAmount }

            paint.color = colorDark
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("$cIdx", 38f, yPos + 11f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(cName, 65f, yPos + 11f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("${trxs.size} Order", 250f, yPos + 11f, paint)
            canvas.drawText("$totalSoldQty item", 360f, yPos + 11f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = colorGreen
            canvas.drawText(rupiahFormat.format(cRevenue), 455f, yPos + 11f, paint)

            yPos += 17f
            cIdx++
        }

        yPos += 15f

        // 4. Rincian Produk Terjual & Keterangan Kasir yang Menjual
        yPos = checkPageBreak(120f, yPos)
        paint.color = colorDark
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("☕ Rincian Produk Terjual & Kasir yang Menjual:", 30f, yPos, paint)
        yPos += 14f

        // Table Header
        paint.color = Color.rgb(235, 235, 235)
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 18f, paint)

        paint.color = colorDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NO", 36f, yPos + 13f, paint)
        canvas.drawText("NAMA MENU / PRODUK", 65f, yPos + 13f, paint)
        canvas.drawText("QTY", 250f, yPos + 13f, paint)
        canvas.drawText("KASIR PENJUAL", 310f, yPos + 13f, paint)
        canvas.drawText("TOTAL OMSET", 455f, yPos + 13f, paint)
        yPos += 20f

        // Aggregate items with cashier who sold
        data class ItemAgg(
            val name: String,
            var qty: Int,
            var total: Double,
            val cashiers: MutableSet<String>
        )
        val aggMap = mutableMapOf<String, ItemAgg>()
        data.items.forEach { itm ->
            val cName = trxIdToCashier[itm.transactionId] ?: "Kasir"
            val existing = aggMap.getOrPut(itm.menuName) {
                ItemAgg(itm.menuName, 0, 0.0, mutableSetOf())
            }
            existing.qty += itm.quantity
            existing.total += itm.subtotal
            existing.cashiers.add(cName)
        }
        val sortedAgg = aggMap.values.sortedByDescending { it.qty }

        var itemIdx = 1
        for (item in sortedAgg) {
            yPos = checkPageBreak(18f, yPos)
            if (itemIdx % 2 == 0) {
                paint.color = Color.rgb(250, 250, 250)
                paint.style = Paint.Style.FILL
                canvas.drawRect(30f, yPos - 2f, (pageWidth - 30).toFloat(), yPos + 15f, paint)
            }

            paint.color = colorDark
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("$itemIdx", 38f, yPos + 11f, paint)
            canvas.drawText(item.name.take(30), 65f, yPos + 11f, paint)
            canvas.drawText("${item.qty}", 250f, yPos + 11f, paint)

            val cashierStr = item.cashiers.joinToString(", ").take(22)
            paint.color = colorBlue
            canvas.drawText(cashierStr, 310f, yPos + 11f, paint)

            paint.color = colorDark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(rupiahFormat.format(item.total), 455f, yPos + 11f, paint)

            yPos += 17f
            itemIdx++
        }

        yPos += 15f

        // 5. Riwayat Pembelian Bahan Baku Periode Ini (Jika Ada)
        if (data.purchases.isNotEmpty()) {
            yPos = checkPageBreak(120f, yPos)
            paint.color = colorDark
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("📦 Riwayat Pembelian Bahan Baku Periode Ini:", 30f, yPos, paint)
            yPos += 14f

            // Table Header
            paint.color = Color.rgb(235, 235, 235)
            paint.style = Paint.Style.FILL
            canvas.drawRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 18f, paint)

            paint.color = colorDark
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("NO", 36f, yPos + 13f, paint)
            canvas.drawText("WAKTU", 60f, yPos + 13f, paint)
            canvas.drawText("BAHAN BAKU", 140f, yPos + 13f, paint)
            canvas.drawText("QTY MASUK", 260f, yPos + 13f, paint)
            canvas.drawText("BIAYA BELANJA", 350f, yPos + 13f, paint)
            canvas.drawText("PETUGAS / KASIR", 450f, yPos + 13f, paint)
            yPos += 20f

            var pIdx = 1
            for (p in data.purchases) {
                yPos = checkPageBreak(18f, yPos)
                if (pIdx % 2 == 0) {
                    paint.color = Color.rgb(250, 250, 250)
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(30f, yPos - 2f, (pageWidth - 30).toFloat(), yPos + 15f, paint)
                }

                paint.color = colorDark
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("$pIdx", 38f, yPos + 11f, paint)
                canvas.drawText(dateTimeFormat.format(Date(p.timestamp)), 60f, yPos + 11f, paint)
                canvas.drawText(p.ingredientName.take(20), 140f, yPos + 11f, paint)
                canvas.drawText("+${p.quantity} ${p.unit}", 260f, yPos + 11f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = colorRed
                canvas.drawText(rupiahFormat.format(p.totalCost), 350f, yPos + 11f, paint)

                paint.color = colorDark
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(p.cashierName.ifBlank { "-" }.take(16), 450f, yPos + 11f, paint)

                yPos += 17f
                pIdx++
            }

            // Total Belanja Row
            yPos = checkPageBreak(22f, yPos)
            paint.color = Color.rgb(255, 235, 238)
            paint.style = Paint.Style.FILL
            canvas.drawRect(30f, yPos - 2f, (pageWidth - 30).toFloat(), yPos + 17f, paint)
            paint.color = colorRed
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText("TOTAL BIAYA PEMBELIAN BAHAN BAKU:", 140f, yPos + 12f, paint)
            canvas.drawText(rupiahFormat.format(data.totalPurchaseCost), 350f, yPos + 12f, paint)
            yPos += 24f
        }

        // 6. Footer & Signatures
        yPos = checkPageBreak(90f, yPos)
        yPos += 15f
        paint.color = colorBorder
        paint.strokeWidth = 1f
        canvas.drawLine(30f, yPos, (pageWidth - 30).toFloat(), yPos, paint)

        yPos += 16f
        paint.color = colorGray
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Laporan POS & Inventori dihasilkan secara otomatis oleh sistem kafe.", 30f, yPos, paint)
        canvas.drawText("Tanda Tangan Penanggung Jawab:", 390f, yPos, paint)

        yPos += 45f
        paint.color = colorDark
        canvas.drawLine(390f, yPos, 540f, yPos, paint)
        paint.textSize = 8.5f
        canvas.drawText("( Owner / Kasir Bertugas )", 400f, yPos + 12f, paint)

        // Draw page number on last page
        paint.color = colorGray
        paint.textSize = 8f
        canvas.drawText("Halaman $pageNumber", (pageWidth - 80).toFloat(), (pageHeight - 20).toFloat(), paint)

        pdfDocument.finishPage(page)

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

    fun generatePurchasesPdf(context: Context, data: PdfPurchaseReportData): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

        val colorPrimary = Color.rgb(255, 107, 0)
        val colorDark = Color.rgb(33, 33, 33)
        val colorGray = Color.rgb(117, 117, 117)
        val colorLightGray = Color.rgb(245, 245, 245)
        val colorBorder = Color.rgb(224, 224, 224)
        val colorRed = Color.rgb(198, 40, 40)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader() {
            paint.color = colorPrimary
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 75f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            canvas.drawText(data.cafeName.ifBlank { "RENCANA KOPI" }, 30f, 32f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(data.cafeAddress.ifBlank { "Sistem Kasir & Manajemen Kafe" }, 30f, 48f, paint)
            canvas.drawText("Telp: ${data.cafePhone.ifBlank { "-" }}  •  Dicetak: ${data.printedDateFormatted}", 30f, 62f, paint)

            paint.color = colorDark
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("LAPORAN RIWAYAT PEMBELIAN BAHAN BAKU", 30f, 98f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = colorGray
            canvas.drawText("Catatan pengadaan, restock stok masuk, dan biaya pembelian bahan kafe", 30f, 112f, paint)
        }

        fun checkPageBreak(requiredHeight: Float, currentY: Float): Float {
            if (currentY + requiredHeight > pageHeight - 65f) {
                paint.color = colorGray
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Halaman $pageNumber", (pageWidth - 80).toFloat(), (pageHeight - 20).toFloat(), paint)

                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                paint.color = colorPrimary
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 30f, paint)
                paint.color = Color.WHITE
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${data.cafeName.ifBlank { "RENCANA KOPI" }} - Pembelian Bahan Baku (Hal $pageNumber)", 30f, 20f, paint)

                return 50f
            }
            return currentY
        }

        drawHeader()
        var yPos = 130f

        // KPI Summary Box
        val boxWidth = (pageWidth - 60 - 20) / 3f
        val boxHeight = 52f
        val uniqueIngredients = data.purchases.map { it.ingredientName }.distinct().size

        drawKpiBox(canvas, paint, 30f, yPos, boxWidth, boxHeight, "TOTAL PEMBELIAN", "${data.purchases.size} Transaksi", colorPrimary, colorLightGray, colorBorder)
        drawKpiBox(canvas, paint, 30f + boxWidth + 10f, yPos, boxWidth, boxHeight, "TOTAL PENGELUARAN", rupiahFormat.format(data.totalCost), colorRed, colorLightGray, colorBorder)
        drawKpiBox(canvas, paint, 30f + (boxWidth + 10f) * 2, yPos, boxWidth, boxHeight, "RAGAM BAHAN", "$uniqueIngredients Macam", colorDark, colorLightGray, colorBorder)

        yPos += boxHeight + 25f

        // Table Header
        paint.color = Color.rgb(235, 235, 235)
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 18f, paint)

        paint.color = colorDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NO", 36f, yPos + 13f, paint)
        canvas.drawText("WAKTU", 58f, yPos + 13f, paint)
        canvas.drawText("NAMA BAHAN", 145f, yPos + 13f, paint)
        canvas.drawText("JUMLAH", 250f, yPos + 13f, paint)
        canvas.drawText("HARGA/UNIT", 320f, yPos + 13f, paint)
        canvas.drawText("TOTAL BIAYA", 400f, yPos + 13f, paint)
        canvas.drawText("PETUGAS/KASIR", 480f, yPos + 13f, paint)
        yPos += 20f

        var idx = 1
        for (p in data.purchases) {
            yPos = checkPageBreak(18f, yPos)
            if (idx % 2 == 0) {
                paint.color = Color.rgb(250, 250, 250)
                paint.style = Paint.Style.FILL
                canvas.drawRect(30f, yPos - 2f, (pageWidth - 30).toFloat(), yPos + 15f, paint)
            }

            paint.color = colorDark
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("$idx", 38f, yPos + 11f, paint)
            canvas.drawText(dateTimeFormat.format(Date(p.timestamp)), 58f, yPos + 11f, paint)
            canvas.drawText(p.ingredientName.take(18), 145f, yPos + 11f, paint)
            canvas.drawText("+${p.quantity} ${p.unit}", 250f, yPos + 11f, paint)
            canvas.drawText(rupiahFormat.format(p.costPerUnit), 320f, yPos + 11f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = colorRed
            canvas.drawText(rupiahFormat.format(p.totalCost), 400f, yPos + 11f, paint)

            paint.color = colorDark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(p.cashierName.ifBlank { "-" }.take(14), 480f, yPos + 11f, paint)

            yPos += 17f
            idx++
        }

        // Summary Total Row
        yPos = checkPageBreak(25f, yPos)
        paint.color = Color.rgb(255, 235, 238)
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, yPos - 2f, (pageWidth - 30).toFloat(), yPos + 18f, paint)

        paint.color = colorRed
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL BIAYA BELANJA BAHAN BAKU:", 145f, yPos + 13f, paint)
        canvas.drawText(rupiahFormat.format(data.totalCost), 400f, yPos + 13f, paint)
        yPos += 30f

        // Footer & Signatures
        yPos = checkPageBreak(90f, yPos)
        paint.color = colorBorder
        paint.strokeWidth = 1f
        canvas.drawLine(30f, yPos, (pageWidth - 30).toFloat(), yPos, paint)

        yPos += 16f
        paint.color = colorGray
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Laporan riwayat pembelian bahan baku ini dibuat untuk verifikasi stok dan pengeluaran modal kafe.", 30f, yPos, paint)
        canvas.drawText("Tanda Tangan Penanggung Jawab:", 390f, yPos, paint)

        yPos += 45f
        paint.color = colorDark
        canvas.drawLine(390f, yPos, 540f, yPos, paint)
        paint.textSize = 8.5f
        canvas.drawText("( Manajer Logistik / Kasir )", 400f, yPos + 12f, paint)

        paint.color = colorGray
        paint.textSize = 8f
        canvas.drawText("Halaman $pageNumber", (pageWidth - 80).toFloat(), (pageHeight - 20).toFloat(), paint)

        pdfDocument.finishPage(page)

        return try {
            val reportDir = File(context.cacheDir, "reports")
            if (!reportDir.exists()) reportDir.mkdirs()
            val fileName = "Riwayat_Pembelian_Bahan_${System.currentTimeMillis()}.pdf"
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
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 8f, y + 16f, paint)

        // Value
        paint.color = valueColor
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 8f, y + 38f, paint)
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
                putExtra(Intent.EXTRA_SUBJECT, "Laporan PDF Kafe")
                putExtra(Intent.EXTRA_TEXT, "Berikut terlampir file dokumen Laporan PDF Kafe.")
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
