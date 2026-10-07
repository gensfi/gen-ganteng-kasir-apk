package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: Long): MemberEntity?

    @Query("SELECT * FROM members WHERE phone = :phone LIMIT 1")
    suspend fun getMemberByPhone(phone: String): MemberEntity?

    @Query("SELECT * FROM members WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchMembers(query: String): Flow<List<MemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("UPDATE members SET points = :newPoints WHERE id = :id")
    suspend fun updatePoints(id: Long, newPoints: Int)

    @Query("UPDATE members SET points = points + :delta, lastTransactionAt = :timestamp WHERE id = :id")
    suspend fun addPoints(id: Long, delta: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE members SET points = MAX(0, points - :delta), lastTransactionAt = :timestamp WHERE id = :id")
    suspend fun deductPoints(id: Long, delta: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM members")
    suspend fun getMemberCount(): Int

    @Query("SELECT COALESCE(SUM(points), 0) FROM members")
    suspend fun getTotalPoints(): Int
}
