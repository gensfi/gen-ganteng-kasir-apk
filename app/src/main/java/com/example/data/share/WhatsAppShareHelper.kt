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
        footerMessage: String,
        remainingPoints: Int? = null
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

        val displayPaymentMethod = when (transaction.paymentMethod.uppercase()) {
            "CASH" -> "Tunai (CASH)"
            "QRIS" -> "QRIS"
            "ONLINE", "PESANAN_ONLINE", "TRANSFER" -> "Pesanan Online"
            else -> transaction.paymentMethod
        }
        sb.append("*TOTAL AKHIR   : ${currency.format(transaction.totalAmount)}*\n")
        sb.append("Metode Bayar   : $displayPaymentMethod\n")

        if (transaction.paymentMethod.equals("CASH", ignoreCase = true)) {
            sb.append("Tunai Diterima : ${currency.format(transaction.cashGiven)}\n")
            sb.append("Kembalian      : ${currency.format(transaction.changeAmount)}\n")
        }

        if (transaction.memberId != null || transaction.pointsEarned > 0 || transaction.pointsRedeemed > 0 || remainingPoints != null) {
            sb.append("----------------------------\n")
            sb.append("⭐ *LOYALITAS MEMBER*\n")
            if (!transaction.customerName.isNullOrBlank()) {
                sb.append("Member         : ${transaction.customerName}\n")
            }
            if (transaction.pointsEarned > 0) {
                sb.append("Poin Diperoleh : +${transaction.pointsEarned} Poin\n")
            }
            if (transaction.pointsRedeemed > 0) {
                sb.append("Poin Ditukar   : -${transaction.pointsRedeemed} Poin (-${currency.format(transaction.pointsRedeemed * 1.0)})\n")
            }
            // Sisa Poin yang dimiliki oleh member
            val sisaPoin = remainingPoints ?: 0
            sb.append("Sisa Poin      : $sisaPoin Poin\n")
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
        val targetNumber = cleanPhoneNumber(phoneNumber)
        val encodedMessage = try {
            URLEncoder.encode(receiptText, "UTF-8")
        } catch (_: Exception) {
            receiptText
        }

        // 1. Try direct WhatsApp link with target phone number
        if (!targetNumber.isNullOrBlank()) {
            try {
                val directUri = Uri.parse("https://api.whatsapp.com/send?phone=$targetNumber&text=$encodedMessage")
                val waIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
                    setPackage("com.whatsapp")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(waIntent)
                return true
            } catch (_: Exception) {
                // WhatsApp app with explicit package not found, try generic view
                try {
                    val genericUri = Uri.parse("https://api.whatsapp.com/send?phone=$targetNumber&text=$encodedMessage")
                    val browserIntent = Intent(Intent.ACTION_VIEW, genericUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                    return true
                } catch (_: Exception) {}
            }
        } else {
            // No phone specified, try WhatsApp share without specific number
            try {
                val uri = Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }

        // 2. Fallback to standard Android share chooser
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, receiptText)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(shareIntent, "Kirim Struk Digital").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
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
