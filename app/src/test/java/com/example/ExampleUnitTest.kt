package com.example

import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.share.WhatsAppShareHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `test WhatsApp receipt format contains required fields`() {
        val trx = TransactionEntity(
            id = "TRX-20261001-0001",
            cashierId = 1L,
            cashierName = "Budi Kasir",
            timestamp = 1700000000000L,
            subtotal = 50000.0,
            taxAmount = 5000.0,
            serviceChargeAmount = 2500.0,
            discountAmount = 0.0,
            totalAmount = 57500.0,
            paymentMethod = "CASH",
            cashGiven = 60000.0,
            changeAmount = 2500.0,
            customerName = "Pak Doni",
            customerPhone = "081234567890"
        )

        val items = listOf(
            TransactionItemEntity(
                transactionId = trx.id,
                menuId = 1L,
                menuName = "Kopi Susu Aren",
                price = 25000.0,
                quantity = 2
            )
        )

        val receipt = WhatsAppShareHelper.buildReceiptText(
            transaction = trx,
            items = items,
            cafeName = "Kopi Kenangan Senja",
            cafeAddress = "Jl. Malioboro No. 45",
            cafePhone = "0812-3456-7890",
            footerMessage = "Terima kasih!"
        )

        assertTrue(receipt.contains("KOPI KENANGAN SENJA"))
        assertTrue(receipt.contains("TRX-20261001-0001"))
        assertTrue(receipt.contains("Kopi Susu Aren"))
        assertTrue(receipt.contains("57.500"))
        assertTrue(receipt.contains("Budi Kasir"))
    }
}
