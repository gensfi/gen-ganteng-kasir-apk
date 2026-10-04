package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.VoucherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers ORDER BY isActive DESC, code ASC")
    fun getAllVouchers(): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers WHERE UPPER(code) = UPPER(:code) LIMIT 1")
    suspend fun getVoucherByCode(code: String): VoucherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: VoucherEntity)

    @Update
    suspend fun updateVoucher(voucher: VoucherEntity)

    @Query("DELETE FROM vouchers WHERE code = :code")
    suspend fun deleteVoucher(code: String)

    @Query("UPDATE vouchers SET usedCount = usedCount + 1 WHERE code = :code")
    suspend fun incrementUsedCount(code: String)
}
