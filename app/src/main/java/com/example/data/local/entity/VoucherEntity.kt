package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey
    val code: String, // e.g. "PROMO20", "KOPI10RB" (Uppercase)
    val discountType: String, // "PERCENT" or "NOMINAL"
    val discountValue: Double, // e.g. 20.0 (20%) or 10000.0 (Rp 10.000)
    val minPurchase: Double = 0.0,
    val quota: Int = 100, // -1 for unlimited
    val usedCount: Int = 0,
    val expiryDate: Long = System.currentTimeMillis() + (365L * 24 * 3600 * 1000), // Default 1 year
    val isActive: Boolean = true
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiryDate
    fun isQuotaExceeded(): Boolean = quota > 0 && usedCount >= quota
    fun isValid(currentSubtotal: Double): Boolean {
        return isActive && !isExpired() && !isQuotaExceeded() && currentSubtotal >= minPurchase
    }
}
