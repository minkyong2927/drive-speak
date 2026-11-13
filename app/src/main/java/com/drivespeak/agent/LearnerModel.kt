package com.drivespeak.agent

/** 한 번의 교정 기록 */
data class ErrorRecord(val type: String, val day: Int, val example: String)

data class Weakness(val type: String, val count: Int, val example: String)

/**
 * 학습자 기록. 교정을 오류 유형별로 쌓아 두고, 에이전트가 다음 연습을 고를 때 읽음.
 * 주차 후 되돌아보기 단계에서 에이전트가 [record] 로 고쳐 씀.
 */
class LearnerModel(private val windowDays: Int = 7, private val focusCount: Int = 3) {

    private val records = mutableListOf<ErrorRecord>()

    fun record(type: String, day: Int, example: String) {
        if (type.isBlank()) return
        records += ErrorRecord(type.trim(), day, example)
    }

    /** 최근 [windowDays]일 안의 유형별 횟수. 많은 순, 같으면 최근 순 */
    fun weaknesses(today: Int): List<Weakness> =
        records.filter { today - it.day in 0 until windowDays }
            .groupBy { it.type }
            .map { (type, list) -> Weakness(type, list.size, list.maxByOrNull { it.day }!!.example) to list.maxOf { it.day } }
            .sortedWith(compareByDescending<Pair<Weakness, Int>> { it.first.count }.thenByDescending { it.second })
            .map { it.first }

    /** 다음 연습에 넣을 문형. 기준 횟수([focusCount]) 이상 반복된 유형 중 가장 잦은 것 */
    fun focus(today: Int): Weakness? = weaknesses(today).firstOrNull { it.count >= focusCount }

    fun count(type: String, today: Int): Int =
        weaknesses(today).firstOrNull { it.type == type }?.count ?: 0
}
