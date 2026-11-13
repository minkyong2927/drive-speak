package com.drivespeak.pleos

/**
 * Pleos Connect SDK 값 → 드라이브 스픽이 쓰는 값.
 * SDK 열거형의 이름(문자열)만 받아서, SDK 없이도 단위 테스트가 돌게 함.
 */
object PleosMapper {

    /** TurnSignalValue(OFF/LEFT/RIGHT) → "off"/"left"/"right" */
    fun turn(name: String?): String = when (name) {
        "LEFT" -> "left"
        "RIGHT" -> "right"
        else -> "off"
    }

    /**
     * WiperState(ON/OFF) + WiperSwitch(단계) → "off"/"low"/"high".
     * 연속 2단 이상을 고속으로 봄. 스위치 값을 모르면 켜짐은 저속으로 둠.
     */
    fun wiper(stateName: String?, switchName: String?): String {
        if (stateName != "ON") return "off"
        val level = switchName?.removePrefix("CONTINUOUS_LEVEL_")?.toIntOrNull()
        return if (level != null && level >= 2) "high" else "low"
    }

    /** VehicleGear → 주차 여부 */
    fun parked(gearName: String?): Boolean = gearName == "GEAR_PARK"

    /** 다음 회전까지 거리(m) → 회전 안내 임박 여부 */
    fun turnNear(distanceM: Double?, nearM: Double = 300.0): Boolean =
        distanceM != null && distanceM >= 0 && distanceM <= nearM

    /**
     * 도착 지연(분). 경로 시작 때 정해진 도착 시각과 지금 예상 도착 시각의 차이.
     * 일찍 도착하면 0.
     */
    fun delayMin(plannedArrivalMs: Long?, nowMs: Long, remainSec: Int?): Int {
        if (plannedArrivalMs == null || remainSec == null) return 0
        val late = nowMs + remainSec * 1000L - plannedArrivalMs
        return if (late <= 0) 0 else (late / 60_000L).toInt()
    }

    /** 하루 중 분(0시 기준) → "HH:mm" */
    fun hhmm(minutesOfDay: Int): String =
        "%02d:%02d".format((minutesOfDay / 60) % 24, minutesOfDay % 60)
}
