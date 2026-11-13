package com.drivespeak.load

import kotlin.math.abs

/**
 * 안전 되먹임: 운전자가 말하는 동안 조향이 잘게 흔들리는지 봄.
 * 조향 방향이 짧은 시간에 여러 번 뒤집히면(조향 반전) 말하기가 운전에 부담이 된다는 신호로 보고,
 * 튜터가 스스로 '듣기만'으로 낮춤. 큰 회전 한 번(한 방향)은 반전이 아니라서 세지 않음.
 */
class StabilityMonitor(
    private val windowMs: Long = 6000,
    /** 이 각도 이상 되돌아가야 반전 한 번으로 셈 (노이즈 제거) */
    private val minSwingDeg: Double = 2.0,
    private val unstableReversals: Int = 5,
) {
    private val reversalTimes = ArrayDeque<Long>()
    private var extreme: Double? = null
    private var direction = 0

    fun add(timeMs: Long, steeringDeg: Double) {
        val ext = extreme
        if (ext == null) {
            extreme = steeringDeg
            return
        }
        val diff = steeringDeg - ext
        when {
            direction == 0 -> if (abs(diff) >= minSwingDeg) {
                direction = if (diff > 0) 1 else -1
                extreme = steeringDeg
            }
            diff * direction > 0 -> extreme = steeringDeg
            abs(diff) >= minSwingDeg -> {
                direction = -direction
                extreme = steeringDeg
                reversalTimes.addLast(timeMs)
            }
        }
        trim(timeMs)
    }

    fun reversals(nowMs: Long): Int {
        trim(nowMs)
        return reversalTimes.size
    }

    fun isUnstable(nowMs: Long): Boolean = reversals(nowMs) >= unstableReversals

    private fun trim(nowMs: Long) {
        while (reversalTimes.isNotEmpty() && nowMs - reversalTimes.first() > windowMs) {
            reversalTimes.removeFirst()
        }
    }
}
