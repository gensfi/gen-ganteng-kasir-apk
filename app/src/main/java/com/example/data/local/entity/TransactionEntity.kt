package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String, // e.g., TRX-20261001-0001
    val cashierId: Long,
    val cashierName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val taxAmount: Double = 0.0,
    val serviceChargeAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String, // CASH, TRANSFER, QRIS
    val cashGiven: Double = 0.0,
    val changeAmount: Double = 0.0,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val notes: String? = null,
    val isSyncedToSheets: Boolean = false
)
