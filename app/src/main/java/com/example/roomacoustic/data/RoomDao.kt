package com.example.roomacoustic.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {

    /* 목록 스트림 */
    @Query("SELECT * FROM rooms ORDER BY id DESC")
    fun getAll(): Flow<List<RoomEntity>>

    /* 단건 조회 */
    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getById(id: Int): RoomEntity

    /* 쓰기 연산 */
    @Insert suspend fun insert(room: RoomEntity): Long
    @Update suspend fun update(room: RoomEntity)
    @Delete suspend fun delete(room: RoomEntity)

    /* ★ 모든 방 삭제 */
    @Query("DELETE FROM rooms")
    suspend fun deleteAll()
}
