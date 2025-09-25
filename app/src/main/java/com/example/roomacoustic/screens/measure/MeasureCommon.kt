package com.example.roomacoustic.screens.measure

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.example.roomacoustic.model.Vec3
import com.example.roomacoustic.util.DepthUtil
import kotlin.math.abs
import kotlin.math.sqrt

/* 3D 거리(m) */
fun distanceMeters(a: Vec3, b: Vec3): Float {
    val dx = a.x - b.x; val dy = a.y - b.y; val dz = a.z - b.z
    return sqrt(dx*dx + dy*dy + dz*dz)
}

/* VIEW 픽셀 → 월드 좌표 (hitTest 우선, 실패 시 RawDepth 역투영) */
fun hitTestOrDepth(frame: Frame, viewX: Float, viewY: Float): Vec3? {
    frame.hitTest(viewX, viewY).firstOrNull()?.let { h ->
        val p = h.hitPose
        return Vec3(p.tx(), p.ty(), p.tz())
    }
    val depth = try { frame.acquireRawDepthImage16Bits() } catch (_: Exception) { null } ?: return null
    return try {
        val inPts  = floatArrayOf(viewX, viewY)
        val outImg = FloatArray(2)
        frame.transformCoordinates2d(Coordinates2d.VIEW, inPts, Coordinates2d.IMAGE_PIXELS, outImg)
        val rect = RectF(outImg[0], outImg[1], outImg[0], outImg[1])
        val w = DepthUtil.bboxCenterToWorld(rect, depth, frame) ?: return null
        Vec3(w[0], w[1], w[2])
    } finally { try { depth.close() } catch (_: Exception) {} }
}

/* 월드 → VIEW 픽셀 */
fun worldToScreen(frame: Frame, p: Vec3, viewW: Float, viewH: Float): Offset? {
    val proj = FloatArray(16)
    val view = FloatArray(16)
    frame.camera.getProjectionMatrix(proj, 0, 0.1f, 100f)
    frame.camera.getViewMatrix(view, 0)

    val world = floatArrayOf(p.x, p.y, p.z, 1f)
    val viewV = FloatArray(4)
    android.opengl.Matrix.multiplyMV(viewV, 0, view, 0, world, 0)
    if (viewV[2] > -0.1f) return null

    val clip = FloatArray(4)
    android.opengl.Matrix.multiplyMV(clip, 0, proj, 0, viewV, 0)
    val w = clip[3]; if (abs(w) < 1e-5f) return null

    val ndcX = clip[0] / w
    val ndcY = clip[1] / w
    val sx = (ndcX * 0.5f + 0.5f) * viewW
    val sy = (1f - (ndcY * 0.5f + 0.5f)) * viewH
    return Offset(sx, sy)
}
