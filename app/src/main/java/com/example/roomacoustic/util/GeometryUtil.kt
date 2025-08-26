package com.example.roomacoustic.util

import com.example.roomacoustic.model.*
import kotlin.math.abs

object GeometryUtil {

    fun computeFrameAndSize(p: PickedPoints): Pair<AxisFrame, Triple<Float, Float, Float>>? {
        val x0 = p.xMin ?: return null
        val x1 = p.xMax ?: return null
        val z0 = p.zMin ?: return null
        val z1 = p.zMax ?: return null
        val y0 = p.yFloor ?: return null
        val y1 = p.yCeil ?: return null

        val vx = (x1 - x0).normalized()
        val vzPrime = (z1 - z0).normalized()
        val proj = vzPrime.dot(vx)
        val vz = (vzPrime - vx * proj).normalized()
        val vy = vx.cross(vz).normalized()

        val w = abs((x1 - x0).dot(vx))
        val d = abs((z1 - z0).dot(vz))
        val h = abs((y1 - y0).dot(vy))

        val cx = x0 + (x1 - x0) * 0.5f
        val cz = z0 + (z1 - z0) * 0.5f
        val cy = y0 + (y1 - y0) * 0.5f
        val center = Vec3((cx.x + cz.x + cy.x) / 3f, (cx.y + cz.y + cy.y) / 3f, (cx.z + cz.z + cy.z) / 3f)

        return AxisFrame(center, vx, vy, vz) to Triple(w, d, h)
    }

    fun validate(frame: AxisFrame, size: Triple<Float, Float, Float>, minLenM: Float = 0.4f): MeasureValidation {
        val (w, d, h) = size
        if (w < minLenM) return MeasureValidation.fail("너비(W)가 너무 짧습니다")
        if (d < minLenM) return MeasureValidation.fail("깊이(D)가 너무 짧습니다")
        if (h < minLenM * 0.5f) return MeasureValidation.fail("높이(H)가 너무 짧습니다")

        val orthoXZ = abs(frame.vx.dot(frame.vz)) < 0.25f
        val orthoXY = abs(frame.vx.dot(frame.vy)) < 0.25f
        val orthoYZ = abs(frame.vy.dot(frame.vz)) < 0.25f
        if (!(orthoXZ && orthoXY && orthoYZ)) return MeasureValidation.fail("축 직교성이 낮습니다. 점을 다시 지정하세요")

        return MeasureValidation.ok()
    }
}
