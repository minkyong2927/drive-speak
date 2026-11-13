package com.drivespeak.agent

import com.drivespeak.load.DrivingLoad

/** 에이전트(LLM)가 고를 수 있는 다음 행동. 뒤로 갈수록 운전자에게 요구가 적음. */
enum class TutorAction { ASK_LONG, ASK_SHORT, LISTEN_ONLY, WAIT }

data class GateResult(val action: TutorAction, val lowered: Boolean, val reason: String)

/**
 * 안전 게이트. 말해도 되는지는 LLM 이 아니라 이 규칙이 정함.
 * 에이전트가 고른 행동이 지금 허용 범위를 넘으면 허용되는 가장 가까운 행동으로 낮춤.
 * LLM 이 틀린 판단을 해도 운전을 방해하는 행동은 나가지 않음.
 */
object SafetyGate {

    /** 지금 허용되는 가장 요구가 큰 행동 */
    fun ceiling(load: DrivingLoad, steeringUnstable: Boolean, busyAhead: Boolean): TutorAction {
        val byLoad = when (load.level) {
            0 -> TutorAction.ASK_LONG
            1 -> TutorAction.ASK_SHORT
            2 -> TutorAction.LISTEN_ONLY
            else -> TutorAction.WAIT
        }
        val bySteering = if (steeringUnstable) TutorAction.LISTEN_ONLY else TutorAction.ASK_LONG
        // 곧 바쁜 구간이면 새 질문을 시작하지 않음
        val byRoute = if (busyAhead) TutorAction.LISTEN_ONLY else TutorAction.ASK_LONG
        return maxOf(byLoad, bySteering, byRoute)
    }

    fun check(
        proposed: TutorAction,
        load: DrivingLoad,
        steeringUnstable: Boolean = false,
        busyAhead: Boolean = false,
    ): GateResult {
        val top = ceiling(load, steeringUnstable, busyAhead)
        if (proposed >= top) return GateResult(proposed, lowered = false, reason = "ALLOWED")
        val reason = when {
            load.level >= 2 -> "DRIVING_LOAD"
            steeringUnstable -> "STEERING_UNSTABLE"
            busyAhead -> "BUSY_AHEAD"
            else -> "DRIVING_LOAD"
        }
        return GateResult(top, lowered = true, reason = reason)
    }
}
