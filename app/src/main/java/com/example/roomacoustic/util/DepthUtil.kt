package com.example.roomacoustic.util

import android.graphics.RectF
import android.media.Image
import com.google.ar.core.CameraIntrinsics
import com.google.ar.core.Frame
import com.google.ar.core.Pose

/**
 * DEPTH16 이미지를 사용해 2-D bbox 중심점을 3-D 월드좌표로 변환.
 *
 * @return FloatArray[3] (x,y,z) 또는 depth=0 → null
 */
object DepthUtil {

    fun bboxCenterToWorld(
        bboxNorm: RectF,      // YOLO 결과, [0–1] 정규화
        depth: Image,         // acquireRawDepthImage16Bits()
        frame: Frame
    ): FloatArray? {

        /* --- 0. bbox 중심 픽셀 좌표(u,v) --- */
        val dims = IntArray(2)
        val intr: CameraIntrinsics = frame.camera.imageIntrinsics
        intr.getImageDimensions(dims, 0)        // dims[0]=w, dims[1]=h
        val u = (bboxNorm.centerX() * dims[0]).toInt()
        val v = (bboxNorm.centerY() * dims[1]).toInt()

        /* --- 1. 16-bit depth 값(mm→m) --- */
        val buf     = depth.planes[0].buffer
        val stride  = depth.planes[0].rowStride / 2          // 16-bit
        val depthMm = buf.getShort(v * stride + u).toInt() and 0xFFFF
        if (depthMm == 0) return null                        // 신뢰도 0
        val d = depthMm / 1000f                              // m

        /* --- 2. 카메라 좌표계(Xc,Yc,Zc) --- */
        val focal = FloatArray(2).also { intr.getFocalLength(it, 0) }
        val c     = FloatArray(2).also { intr.getPrincipalPoint(it, 0) }
        val xc =  (u - c[0]) / focal[0] * d
        val yc = -(v - c[1]) / focal[1] * d           // ARCore: +Y up
        val zc =  d

        /* --- 3. 월드 좌표 --- */
        val worldPose: Pose = frame.camera.pose.compose(Pose.makeTranslation(xc, yc, zc))
        return worldPose.translation     // FloatArray[3]
    }
}
