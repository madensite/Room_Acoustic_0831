package com.example.roomacoustic.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.IOException

object ImageUtils {
    /**
     * BitMap → Pictures/ 에 (fileName).jpg 저장 후 Uri 반환
     */
    fun saveBitmapToPictures(ctx: Context, fileName: String, bmp: Bitmap): Uri {
        val r = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = r.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("이미지 저장 실패")

        r.openOutputStream(uri)!!.use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
        r.update(uri, values, null, null)
        return uri
    }
}
