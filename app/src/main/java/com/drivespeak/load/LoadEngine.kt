package com.drivespeak.load

import kotlin.math.abs

/**
 * 규칙 기반 여유도 판단. 위에서부터 먼저 맞는 규칙이 이김.
 * 상태가 없어서(같은 입력 → 같은 출력) 단위 테스트로 검증함.
 */
class LoadEngine(private val config: LoadConfig = LoadConfig()) {

    fun evaluate(s: VehicleSignals): LoadResult = when {
        s.parked -> LoadResult(DrivingLoad.STOPPED, LoadReason.PARKED)
        s.turnSignal != TurnSignal.OFF -> LoadResult(DrivingLoad.BUSY, LoadReason.TURN_SIGNAL)
        abs(s.steeringAngleDeg) >= config.largeSteeringDeg ->
            LoadResult(DrivingLoad.BUSY, LoadReason.LARGE_STEERING)
        s.decelerationMps2 >= config.hardBrakingMps2 ->
            LoadResult(DrivingLoad.BUSY, LoadReason.HARD_BRAKING)
        s.navGuidance == NavGuidance.VOICE -> LoadResult(DrivingLoad.BUSY, LoadReason.NAV_VOICE)
        isTurnNear(s) -> LoadResult(DrivingLoad.MODERATE, LoadReason.NAV_TURN_NEAR)
        s.wiperHigh -> LoadResult(DrivingLoad.MODERATE, LoadReason.WIPER_HIGH)
        s.speedKmh < config.stoppedSpeedKmh -> LoadResult(DrivingLoad.STOPPED, LoadReason.STOPPED)
        else -> LoadResult(DrivingLoad.RELAXED, LoadReason.CRUISING)
    }

    private fun isTurnNear(s: VehicleSignals): Boolean {
        if (s.navGuidance != NavGuidance.TURN_AHEAD) return false
        val d = s.navDistanceM ?: return true
        return d <= config.navNearDistanceM
    }
}
