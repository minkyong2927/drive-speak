package com.drivespeak.review

import kotlin.math.abs

/**
 * 장소 복습: 틀린 표현을 '틀린 자리'에 묶어 두고, 다음에 같은 자리를 지날 때 다시 물음.
 * 매일 같은 길을 달리는 통근이라서 가능한 복습 방식 (장소가 기억의 단서).
 * 맞히면 간격을 늘리고(1 → 2 → 4 → 7일), 틀리면 다음 날 다시.
 */
data class ReviewItem(
    val id: String,
    /** 경로 시작점부터의 거리 */
    val distanceM: Double,
    val wrong: String,
    val correct: String,
    /** 마지막으로 배우거나 물어본 날 (날짜 번호) */
    val lastDay: Int,
    val streak: Int = 0,
)

class PlaceReview(private val radiusM: Double = 150.0) {
    private val items = LinkedHashMap<String, ReviewItem>()

    fun add(item: ReviewItem) {
        items[item.id] = item
    }

    fun all(): List<ReviewItem> = items.values.toList()

    /** 지금 위치에서 오늘 물어볼 표현. 없으면 null. */
    fun due(distanceM: Double, today: Int): ReviewItem? =
        items.values.firstOrNull {
            abs(it.distanceM - distanceM) <= radiusM && today >= it.lastDay + intervalDays(it.streak)
        }

    /** 답한 결과 반영 */
    fun answered(id: String, correct: Boolean, today: Int) {
        val item = items[id] ?: return
        items[id] = item.copy(lastDay = today, streak = if (correct) item.streak + 1 else 0)
    }

    companion object {
        fun intervalDays(streak: Int): Int = when (streak) {
            0 -> 1
            1 -> 2
            2 -> 4
            else -> 7
        }
    }
}
