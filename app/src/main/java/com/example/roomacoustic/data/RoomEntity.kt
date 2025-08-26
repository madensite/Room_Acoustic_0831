package com.example.roomacoustic.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,

    /* 측정 */
    val hasMeasure: Boolean = false,
    val measureUpdatedAt: Long? = null,

    /* 대화 */
    val hasChat: Boolean = false,
    val lastChatPreview: String? = null,
    val chatUpdatedAt: Long? = null
)
