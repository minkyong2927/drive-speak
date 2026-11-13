package com.drivespeak.route

import com.drivespeak.load.DrivingLoad

/**
 * 같은 길(출퇴근 경로)을 반복해서 달린 기록으로 구간별 운전 여유를 미리 아는 기억 장치.
 * 경로를 segmentM 길이 구간으로 나누고, 주행마다 구간별로 '바빴는지', '얼마나 서 있었는지'를 쌓음.
 *
 * 쓰임
 * - 곧 바쁜 구간(합류·회전)이 오면 튜터가 새 질문을 시작하지 않음 → 문장이 중간에 끊기지 않음
 * - 오래 서는 신호가 오면 긴 말하기 미션을 그 자리에 예약
 */
data class DriveSample(
    /** 경로 시작점부터의 거리 (NaviHelper 남은 거리로 계산) */
    val distanceM: Double,
    val load: DrivingLoad,
    /** 이 표본 시점에 정차해 있던 시간 */
    val stoppedMs: Long = 0,
)

data class BusyAhead(val secondsAhead: Int, val busyDrives: Int, val totalDrives: Int)

data class StopAhead(val distanceAheadM: Double, val averageStopSec: Int)

class RouteMemory(
    private val segmentM: Int = 200,
    /** 이 횟수 이상 달린 구간만 예측에 씀 */
    private val minDrives: Int = 3,
    /** 바빴던 비율이 이 값 이상이면 '바쁜 구간' */
    private val busyRate: Double = 0.7,
    /** 평균 정차가 이 시간 이상이면 '긴 말하기 가능한 신호' */
    private val longStopSec: Int = 30,
) {
    private class Stat {
        var drives = 0
        var busyDrives = 0
        var stopMsTotal = 0L
    }

    private val stats = HashMap<Int, Stat>()

    private fun segmentOf(distanceM: Double) = (distanceM / segmentM).toInt()

    /** 주행 한 번의 기록을 더함 */
    fun recordDrive(samples: List<DriveSample>) {
        val bySegment = samples.groupBy { segmentOf(it.distanceM) }
        for ((seg, list) in bySegment) {
            val st = stats.getOrPut(seg) { Stat() }
            st.drives++
            if (list.any { it.load == DrivingLoad.BUSY }) st.busyDrives++
            st.stopMsTotal += list.sumOf { it.stoppedMs }
        }
    }

    fun drivesAt(distanceM: Double): Int = stats[segmentOf(distanceM)]?.drives ?: 0

    private fun isBusySegment(st: Stat) =
        st.drives >= minDrives && st.busyDrives.toDouble() / st.drives >= busyRate

    /** lookaheadSec 안에 도달할 '바쁜 구간'. 지금 있는 구간은 뺌(이미 여유도 엔진이 판단 중). */
    fun busyAhead(distanceM: Double, speedKmh: Double, lookaheadSec: Int = 30): BusyAhead? {
        if (speedKmh < 1.0) return null
        val mps = speedKmh / 3.6
        val here = segmentOf(distanceM)
        val last = segmentOf(distanceM + mps * lookaheadSec)
        for (seg in (here + 1)..last) {
            val st = stats[seg] ?: continue
            if (!isBusySegment(st)) continue
            val ahead = seg * segmentM - distanceM
            return BusyAhead((ahead / mps).toInt(), st.busyDrives, st.drives)
        }
        return null
    }

    /** withinM 안에서 가장 가까운 '오래 서는 신호' */
    fun longStopAhead(distanceM: Double, withinM: Double = 1000.0): StopAhead? {
        val here = segmentOf(distanceM)
        val last = segmentOf(distanceM + withinM)
        for (seg in (here + 1)..last) {
            val st = stats[seg] ?: continue
            if (st.drives < minDrives) continue
            val avgSec = (st.stopMsTotal / st.drives / 1000).toInt()
            if (avgSec >= longStopSec) return StopAhead(seg * segmentM - distanceM, avgSec)
        }
        return null
    }
}
