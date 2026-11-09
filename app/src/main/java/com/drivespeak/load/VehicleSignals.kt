package com.drivespeak.load

enum class TurnSignal { OFF, LEFT, RIGHT, HAZARD }

enum class NavGuidance {
    /** 안내 없음 */
    NONE,
    /** 회전 안내 예정 (거리는 navDistanceM) */
    TURN_AHEAD,
    /** 내비 안내 음성 출력 중 */
    VOICE
}

/**
 * 여유도 판단에 쓰는 차량 신호 한 시점의 값.
 * Pleos Connect SDK의 Vehicle·NaviHelper·FusedLocation 값을 여기에 채움.
 */
data class VehicleSignals(
    val parked: Boolean = false,
    val speedKmh: Double = 0.0,
    val turnSignal: TurnSignal = TurnSignal.OFF,
    val steeringAngleDeg: Double = 0.0,
    /** 감속도 m/s². 양수가 감속. */
    val decelerationMps2: Double = 0.0,
    val wiperHigh: Boolean = false,
    val navGuidance: NavGuidance = NavGuidance.NONE,
    val navDistanceM: Double? = null,
)

/** 임계값은 코드에 박지 않고 설정값으로 분리 (실차 데이터로 조정). */
data class LoadConfig(
    val largeSteeringDeg: Double = 30.0,
    val hardBrakingMps2: Double = 4.0,
    val navNearDistanceM: Double = 300.0,
    val stoppedSpeedKmh: Double = 1.0,
    /** 바쁨 → 낮은 단계로 내려갈 때 이 시간만큼 안정돼야 함 */
    val calmDownMs: Long = 1500,
)
