package com.example.roomacoustic.repo

import com.example.roomacoustic.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomRepository(private val dao: RoomDao) {

    val rooms = dao.getAll()

    /* 새 방 추가 */
    suspend fun add(title: String) = withContext(Dispatchers.IO) {
        dao.insert(RoomEntity(title = title)).toInt()
    }

    /* 이름 변경 */
    suspend fun rename(id: Int, newTitle: String) = withContext(Dispatchers.IO) {
        val current = dao.getById(id)
        dao.update(current.copy(title = newTitle))
    }

    /* 측정 완료/초기화 */
    suspend fun setMeasure(id: Int, flag: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val current = dao.getById(id)
        dao.update(
            current.copy(
                hasMeasure      = flag,
                measureUpdatedAt = if (flag) now else null
            )
        )
    }

    /* 대화 완료/초기화 */
    suspend fun setChat(id: Int, flag: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val current = dao.getById(id)
        dao.update(
            current.copy(
                hasChat       = flag,
                chatUpdatedAt = if (flag) now else null,
                lastChatPreview = if (flag) current.lastChatPreview else null
            )
        )
    }

    /* 삭제 */
    suspend fun delete(room: RoomEntity) = withContext(Dispatchers.IO) {
        dao.delete(room)
    }

    suspend fun deleteAll() = dao.deleteAll()
}
