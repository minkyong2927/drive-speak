package com.drivespeak.situation

/**
 * 오늘 운전 자체를 수업 재료로 바꾸는 상황 감지기.
 * 차량 신호의 변화(도착 지연, 와이퍼, 연료, 동승자, 주차장 진입)를 상황 하나로 묶고,
 * 실제 값(몇 분 늦는지, 연료 몇 %)을 같이 넘겨 튜터가 그 숫자로 상황극을 만들게 함.
 *
 * - 한 번의 운전에서 같은 상황은 한 번만 만듦 (fired 목록)
 * - 상황을 언제 연습할지는 여유도(LoadEngine)가 정함: 바쁨이면 대기, 보통이면 듣기 버전
 */
enum class Situation {
    /** 도착 예정 시간이 처음 계획보다 lateMinutes 이상 늦어짐 → 지각 연락 상황극 */
    LATE,
    /** 와이퍼 작동 → 날씨 스몰톡 */
    RAIN,
    /** 연료 잔량 낮음 → 주유소 표현 */
    LOW_FUEL,
    /** 동승석·뒷좌석 착석 → 함께하는 퀴즈 */
    PASSENGER,
    /** 주차장 진입 → 주차 정산 표현 */
    PARKING,
}

enum class Wiper { OFF, LOW, HIGH }

enum class PlaceType { NONE, PARKING, GAS_STATION }

/** 상황 판단에 쓰는 신호 한 시점의 값 (NaviHelper·Vehicle·FusedLocation) */
data class SituationSignals(
    val plannedArrivalMin: Int? = null,
    val expectedArrivalMin: Int? = null,
    val wiper: Wiper = Wiper.OFF,
    val fuelPercent: Int = 100,
    val passengerSeated: Boolean = false,
    val place: PlaceType = PlaceType.NONE,
)

data class SituationConfig(
    val lateMinutes: Int = 5,
    val lowFuelPercent: Int = 15,
)

/** value: 상황극에 들어갈 실제 값 (LATE=늦는 분, LOW_FUEL=연료 %, RAIN=2 고속/1 저속) */
data class SituationEvent(val situation: Situation, val value: Int)

class SituationDetector(private val config: SituationConfig = SituationConfig()) {

    private val fired = mutableSetOf<Situation>()

    /** 이번 신호로 새로 생긴 상황만 돌려줌. 우선순위 순서. */
    fun detect(s: SituationSignals): List<SituationEvent> {
        val out = mutableListOf<SituationEvent>()
        fun add(sit: Situation, value: Int) {
            if (fired.add(sit)) out += SituationEvent(sit, value)
        }
        val delay = delayMinutes(s)
        if (delay != null && delay >= config.lateMinutes) add(Situation.LATE, delay)
        if (s.wiper != Wiper.OFF) add(Situation.RAIN, if (s.wiper == Wiper.HIGH) 2 else 1)
        if (s.fuelPercent <= config.lowFuelPercent) add(Situation.LOW_FUEL, s.fuelPercent)
        if (s.passengerSeated) add(Situation.PASSENGER, 1)
        if (s.place == PlaceType.PARKING) add(Situation.PARKING, 0)
        return out
    }

    /** 새 운전(시동)마다 초기화 */
    fun reset() = fired.clear()

    private fun delayMinutes(s: SituationSignals): Int? {
        val plan = s.plannedArrivalMin ?: return null
        val now = s.expectedArrivalMin ?: return null
        return now - plan
    }
}
