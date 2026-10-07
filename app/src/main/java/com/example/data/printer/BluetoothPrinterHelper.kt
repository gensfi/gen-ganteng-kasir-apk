package com.example.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean
)

sealed class PrinterStatus {
    object Idle : PrinterStatus()
    object Connecting : PrinterStatus()
    data class Connected(val deviceName: String) : PrinterStatus()
    data class Error(val message: String) : PrinterStatus()
    data class Success(val message: String) : PrinterStatus()
}

class BluetoothPrinterHelper(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var connectedSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    companion object {
        // Standard Serial Port Profile (SPP) UUID for Bluetooth POS printers
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // ESC/POS Command Constants
        val ESC: Byte = 0x1B
        val GS: Byte = 0x1D
        val LF: Byte = 0x0A

        val CMD_INIT = byteArrayOf(ESC, 0x40) // Initialize printer
        val CMD_ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
        val CMD_ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
        val CMD_ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02)

        val CMD_BOLD_ON = byteArrayOf(ESC, 0x45, 0x01)
        val CMD_BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)

        val CMD_FONT_NORMAL = byteArrayOf(GS, 0x21, 0x00)
        val CMD_FONT_DOUBLE_HEIGHT = byteArrayOf(GS, 0x21, 0x01)
        val CMD_FONT_DOUBLE_WIDTH = byteArrayOf(GS, 0x21, 0x10)
        val CMD_FONT_DOUBLE = byteArrayOf(GS, 0x21, 0x11)

        val CMD_FEED_AND_CUT = byteArrayOf(GS, 0x56, 0x41, 0x00)
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothPrinterDevice> {
        val list = mutableListOf<BluetoothPrinterDevice>()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return list

        try {
            bluetoothAdapter.bondedDevices?.forEach { device ->
                list.add(
                    BluetoothPrinterDevice(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        isBonded = true
                    )
                )
            }
        } catch (e: SecurityException) {
            // Handled when permission is not yet granted
        }
        return list
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Result<String> = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth tidak aktif."))
        }

        try {
            disconnect()
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothAdapter.cancelDiscovery()
            socket.connect()

            connectedSocket = socket
            outputStream = socket.outputStream
            Result.success(device.name ?: deviceAddress)
        } catch (e: Exception) {
            disconnect()
            Result.failure(Exception("Gagal terhubung ke printer: ${e.localizedMessage}"))
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            connectedSocket?.close()
        } catch (_: IOException) {}
        outputStream = null
        connectedSocket = null
    }

    fun isConnected(): Boolean {
        return connectedSocket?.isConnected == true && outputStream != null
    }

    suspend fun printTestReceipt(
        cafeName: String,
        paperSize: Int = 58 // 58 or 80 mm
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream ?: return@withContext Result.failure(Exception("Printer belum terhubung."))
        val maxChars = if (paperSize == 80) 48 else 32

        try {
            val bytes = mutableListOf<Byte>()
            bytes.addAll(CMD_INIT.toList())

            // Header
            bytes.addAll(CMD_ALIGN_CENTER.toList())
            bytes.addAll(CMD_BOLD_ON.toList())
            bytes.addAll(CMD_FONT_DOUBLE.toList())
            bytes.addAll("$cafeName\n".toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll(CMD_FONT_NORMAL.toList())
            bytes.addAll(CMD_BOLD_OFF.toList())
            bytes.addAll("TEST PRINT BERHASIL\n".toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll(formatDivider('-', maxChars).toByteArray(Charsets.ISO_8859_1).toList())

            // Details
            bytes.addAll(CMD_ALIGN_LEFT.toList())
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID")).format(Date())
            bytes.addAll("Waktu : $dateStr\n".toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll("Ukuran: ${paperSize}mm ($maxChars kolom)\n".toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll("Status: Terhubung via Bluetooth\n".toByteArray(Charsets.ISO_8859_1).toList())

            bytes.addAll(CMD_ALIGN_CENTER.toList())
            bytes.addAll(formatDivider('=', maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll("ESC/POS Thermal Printer Ready\n\n\n\n".toByteArray(Charsets.ISO_8859_1).toList())

            stream.write(bytes.toByteArray())
            stream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun printCustomText(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream ?: return@withContext Result.failure(Exception("Printer belum terhubung."))
        try {
            val bytes = mutableListOf<Byte>()
            bytes.addAll(CMD_INIT.toList())
            bytes.addAll(CMD_ALIGN_LEFT.toList())
            bytes.addAll(text.toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll("\n\n\n\n".toByteArray(Charsets.ISO_8859_1).toList())
            stream.write(bytes.toByteArray())
            stream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun printTransactionReceipt(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        cafeName: String,
        cafeAddress: String,
        cafePhone: String,
        footerMessage: String,
        paperSize: Int = 58,
        logoPath: String? = null,
        remainingPoints: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream ?: return@withContext Result.failure(Exception("Printer belum terhubung."))
        val maxChars = if (paperSize == 80) 48 else 32

        try {
            val bytes = mutableListOf<Byte>()
            val currency = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                maximumFractionDigits = 0
            }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

            bytes.addAll(CMD_INIT.toList())

            // 0. Print Logo if available
            if (!logoPath.isNullOrBlank()) {
                try {
                    val file = java.io.File(logoPath)
                    if (file.exists()) {
                        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) {
                            val targetWidth = if (paperSize == 80) 256 else 192
                            val targetHeight = ((bmp.height.toFloat() / bmp.width.toFloat()) * targetWidth).toInt().coerceAtLeast(32)
                            val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
                            val rasterBytes = decodeBitmapToEscPosRaster(scaled)
                            bytes.addAll(CMD_ALIGN_CENTER.toList())
                            bytes.addAll(rasterBytes.toList())
                            bytes.addAll("\n".toByteArray(Charsets.ISO_8859_1).toList())
                        }
                    }
                } catch (_: Exception) {}
            }

            // 1. Cafe Header (Center, Bold, Large)
            bytes.addAll(CMD_ALIGN_CENTER.toList())
            bytes.addAll(CMD_BOLD_ON.toList())
            bytes.addAll(CMD_FONT_DOUBLE.toList())
            bytes.addAll("$cafeName\n".toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll(CMD_FONT_NORMAL.toList())
            bytes.addAll(CMD_BOLD_OFF.toList())

            if (cafeAddress.isNotBlank()) {
                bytes.addAll("$cafeAddress\n".toByteArray(Charsets.ISO_8859_1).toList())
            }
            if (cafePhone.isNotBlank()) {
                bytes.addAll("Telp: $cafePhone\n".toByteArray(Charsets.ISO_8859_1).toList())
            }

            bytes.addAll(formatDivider('=', maxChars).toByteArray(Charsets.ISO_8859_1).toList())

            // 2. Transaction Info (Left Align)
            bytes.addAll(CMD_ALIGN_LEFT.toList())
            bytes.addAll(formatKeyValue("No. Struk", transaction.id, maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll(formatKeyValue("Tanggal", dateFormat.format(Date(transaction.timestamp)), maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll(formatKeyValue("Kasir", transaction.cashierName, maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            if (!transaction.customerName.isNullOrBlank()) {
                bytes.addAll(formatKeyValue("Pelanggan", transaction.customerName, maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            }

            bytes.addAll(formatDivider('-', maxChars).toByteArray(Charsets.ISO_8859_1).toList())

            // 3. Items list
            items.forEach { item ->
                val lineTotal = item.price * item.quantity
                val lineTotalStr = currency.format(lineTotal)
                val itemHeader = "${item.quantity}x ${item.menuName}"

                if (itemHeader.length + lineTotalStr.length + 1 <= maxChars) {
                    bytes.addAll(formatKeyValue(itemHeader, lineTotalStr, maxChars).toByteArray(Charsets.ISO_8859_1).toList())
                } else {
                    bytes.addAll("$itemHeader\n".toByteArray(Charsets.ISO_8859_1).toList())
                    val priceDetail = "@${currency.format(item.price)}"
                    bytes.addAll(formatKeyValue("  $priceDetail", lineTotalStr, maxChars).toByteArray(Charsets.ISO_8859_1).toList())
                }

                if (!item.itemNotes.isNullOrBlank()) {
                    bytes.addAll("  * ${item.itemNotes}\n".toByteArray(Charsets.ISO_8859_1).toList())
                }
            }

            bytes.addAll(formatDivider('-', maxChars).toByteArray(Charsets.ISO_8859_1).toList())

            // 4. Totals & Payment
            bytes.addAll(formatKeyValue("Subtotal", currency.format(transaction.subtotal), maxChars).toByteArray(Charsets.ISO_8859_1).toList())

            if (transaction.discountAmount > 0) {
                bytes.addAll(formatKeyValue("Diskon", "-${currency.format(transaction.discountAmount)}", maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            }
            if (transaction.serviceChargeAmount > 0) {
                bytes.addAll(formatKeyValue("Biaya Layanan", currency.format(transaction.serviceChargeAmount), maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            }
            if (transaction.taxAmount > 0) {
                bytes.addAll(formatKeyValue("Pajak (PB1)", currency.format(transaction.taxAmount), maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            }

            bytes.addAll(CMD_BOLD_ON.toList())
            bytes.addAll(formatKeyValue("TOTAL", currency.format(transaction.totalAmount), maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            bytes.addAll(CMD_BOLD_OFF.toList())

            val displayMethod = when (transaction.paymentMethod.uppercase()) {
                "CASH" -> "Tunai (CASH)"
                "QRIS" -> "QRIS"
                "ONLINE", "PESANAN_ONLINE", "TRANSFER" -> "Pesanan Online"
                else -> transaction.paymentMethod
            }
            bytes.addAll(formatKeyValue("Metode Bayar", displayMethod, maxChars).toByteArray(Charsets.ISO_8859_1).toList())

            if (transaction.paymentMethod.equals("CASH", ignoreCase = true)) {
                bytes.addAll(formatKeyValue("Tunai Diterima", currency.format(transaction.cashGiven), maxChars).toByteArray(Charsets.ISO_8859_1).toList())
                bytes.addAll(formatKeyValue("Kembalian", currency.format(transaction.changeAmount), maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            }

            // Member Loyalty Info
            if (transaction.memberId != null || transaction.pointsEarned > 0 || transaction.pointsRedeemed > 0 || remainingPoints != null) {
                bytes.addAll(formatDivider('-', maxChars).toByteArray(Charsets.ISO_8859_1).toList())
                bytes.addAll(CMD_BOLD_ON.toList())
                bytes.addAll("LOYALITAS MEMBER\n".toByteArray(Charsets.ISO_8859_1).toList())
                bytes.addAll(CMD_BOLD_OFF.toList())
                if (!transaction.customerName.isNullOrBlank()) {
                    bytes.addAll(formatKeyValue("Member", transaction.customerName, maxChars).toByteArray(Charsets.ISO_8859_1).toList())
                }
                if (transaction.pointsEarned > 0) {
                    bytes.addAll(formatKeyValue("Poin Diperoleh", "+${transaction.pointsEarned}", maxChars).toByteArray(Charsets.ISO_8859_1).toList())
                }
                if (transaction.pointsRedeemed > 0) {
                    bytes.addAll(formatKeyValue("Poin Digunakan", "-${transaction.pointsRedeemed}", maxChars).toByteArray(Charsets.ISO_8859_1).toList())
                }
                val sisaPoin = remainingPoints ?: 0
                bytes.addAll(formatKeyValue("Sisa Poin", "$sisaPoin Poin", maxChars).toByteArray(Charsets.ISO_8859_1).toList())
            }

            bytes.addAll(formatDivider('=', maxChars).toByteArray(Charsets.ISO_8859_1).toList())

            // 5. Footer Message
            bytes.addAll(CMD_ALIGN_CENTER.toList())
            if (footerMessage.isNotBlank()) {
                footerMessage.split("\n").forEach { line ->
                    bytes.addAll("$line\n".toByteArray(Charsets.ISO_8859_1).toList())
                }
            } else {
                bytes.addAll("Terima kasih atas kunjungan Anda!\n".toByteArray(Charsets.ISO_8859_1).toList())
            }

            bytes.addAll("\n\n\n\n".toByteArray(Charsets.ISO_8859_1).toList())

            stream.write(bytes.toByteArray())
            stream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatDivider(char: Char, width: Int): String {
        return char.toString().repeat(width) + "\n"
    }

    private fun formatKeyValue(key: String, value: String, width: Int): String {
        val spacesNeeded = width - key.length - value.length
        return if (spacesNeeded > 0) {
            key + " ".repeat(spacesNeeded) + value + "\n"
        } else {
            "$key\n" + " ".repeat(maxOf(0, width - value.length)) + value + "\n"
        }
    }

    private fun decodeBitmapToEscPosRaster(bitmap: android.graphics.Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        val bytes = ByteArray(8 + widthBytes * height)
        bytes[0] = 0x1D
        bytes[1] = 0x76
        bytes[2] = 0x30
        bytes[3] = 0x00 // normal mode
        bytes[4] = (widthBytes and 0xFF).toByte()
        bytes[5] = ((widthBytes shr 8) and 0xFF).toByte()
        bytes[6] = (height and 0xFF).toByte()
        bytes[7] = ((height shr 8) and 0xFF).toByte()

        var idx = 8
        for (y in 0 until height) {
            for (xb in 0 until widthBytes) {
                var b = 0
                for (bit in 0 until 8) {
                    val x = xb * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val bl = pixel and 0xFF
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * bl).toInt()
                        if (luminance < 128) {
                            b = b or (1 shl (7 - bit))
                        }
                    }
                }
                bytes[idx++] = b.toByte()
            }
        }
        return bytes
    }
}
