package com.example.roomacoustic.util

import com.google.ar.core.Pose
import kotlin.math.*

object AngleUtils {
    /** Pose → yaw(°) 0-360, 시계방향 + */
    fun yawDeg(pose: Pose): Float {
        val q = pose.rotationQuaternion
        val sinyCosp = 2f * (q[3] * q[1] + q[0] * q[2])
        val cosyCosp = 1f - 2f * (q[1] * q[1] + q[2] * q[2])
        var yaw = Math.toDegrees(atan2(sinyCosp, cosyCosp).toDouble()).toFloat()
        if (yaw < 0) yaw += 360f
        return yaw
    }
    /** -180 ~ 180° 차이 */
    fun diffDeg(a: Float, b: Float): Float = ((b - a + 540f) % 360f) - 180f
}
