package com.example.data.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppShareHelper {

    fun buildReceiptText(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        cafeName: String,
        cafeAddress: String,
        cafePhone: String,
        footerMessage: String
    ): String {
        val currency = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        val dateStr = dateFormat.format(Date(transaction.timestamp))

        val sb = StringBuilder()
        sb.append("☕ *${cafeName.uppercase()}* ☕\n")
        if (cafeAddress.isNotBlank()) sb.append("${cafeAddress}\n")
        if (cafePhone.isNotBlank()) sb.append("Telp: ${cafePhone}\n")
        sb.append("============================\n")
        sb.append("🧾 *STRUK PEMBELIAN RESMI*\n")
        sb.append("No. Transaksi : `${transaction.id}`\n")
        sb.append("Tanggal       : $dateStr\n")
        sb.append("Kasir         : ${transaction.cashierName}\n")
        if (!transaction.customerName.isNullOrBlank()) {
            sb.append("Pelanggan     : ${transaction.customerName}\n")
        }
        sb.append("----------------------------\n")

        items.forEach { item ->
            val totalLine = currency.format(item.price * item.quantity)
            sb.append("• *${item.quantity}x ${item.menuName}*\n")
            sb.append("   @${currency.format(item.price)} = $totalLine\n")
            if (!item.itemNotes.isNullOrBlank()) {
                sb.append("   _(Catatan: ${item.itemNotes})_\n")
            }
        }

        sb.append("----------------------------\n")
        sb.append("Subtotal       : ${currency.format(transaction.subtotal)}\n")

        if (transaction.discountAmount > 0) {
            sb.append("Diskon         : -${currency.format(transaction.discountAmount)}\n")
        }
        if (transaction.serviceChargeAmount > 0) {
            sb.append("Biaya Layanan  : ${currency.format(transaction.serviceChargeAmount)}\n")
        }
        if (transaction.taxAmount > 0) {
            sb.append("Pajak (PB1)    : ${currency.format(transaction.taxAmount)}\n")
        }

        sb.append("*TOTAL AKHIR   : ${currency.format(transaction.totalAmount)}*\n")
        sb.append("Metode Bayar   : ${transaction.paymentMethod}\n")

        if (transaction.paymentMethod.equals("CASH", ignoreCase = true)) {
            sb.append("Tunai Diterima : ${currency.format(transaction.cashGiven)}\n")
            sb.append("Kembalian      : ${currency.format(transaction.changeAmount)}\n")
        }

        sb.append("============================\n")
        if (footerMessage.isNotBlank()) {
            sb.append("$footerMessage\n")
        } else {
            sb.append("Terima kasih telah berkunjung!\n")
        }

        return sb.toString()
    }

    fun shareToWhatsApp(
        context: Context,
        phoneNumber: String?,
        receiptText: String
    ): Boolean {
        return try {
            val encodedMessage = URLEncoder.encode(receiptText, "UTF-8")
            val targetNumber = cleanPhoneNumber(phoneNumber)

            val uri = if (!targetNumber.isNullOrBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$targetNumber&text=$encodedMessage")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback to standard share sheet
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, receiptText)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Kirim Struk Digital"))
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun cleanPhoneNumber(phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("08") -> "628" + digits.substring(2)
            digits.startsWith("8") -> "628" + digits.substring(1)
            digits.startsWith("62") -> digits
            else -> digits
        }
    }
}
