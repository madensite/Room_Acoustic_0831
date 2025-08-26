package com.example.roomacoustic.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * ① roomId→이미지 Uri  임시 캐시 (메모리 + FileProvider 불필요한 app 내부 파일)
 * ② 저장 · 조회 기능만 제공
 */
object SpeakerShotCache {
    private val map = ConcurrentHashMap<Int, Uri>()

    /** Bitmap을 cacheDir/room_<id>.jpg 에 저장하고 Uri 를 기억. */
    fun save(context: Context, roomId: Int, bmp: Bitmap): Uri {
        val file = File(context.cacheDir, "room_${roomId}.jpg")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.toUri().also { map[roomId] = it }
    }

    /** RenderScreen 에서 roomId 로 Uri 조회 */
    fun get(roomId: Int?): Uri? = map[roomId]
}
