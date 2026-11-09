package com.drivespeak.load

/**
 * 신호 변화를 시간 순서대로 받아 튜터에 넘길 단계를 정함.
 * - 올라갈 때(더 바빠짐): 즉시 반영 → 튜터가 바로 멈춤
 * - 내려갈 때(덜 바빠짐): calmDownMs 동안 계속 낮게 유지돼야 반영 → 지시등 깜빡임·짧은 조향에 흔들리지 않음
 */
class LoadTracker(
    private val engine: LoadEngine = LoadEngine(),
    private val config: LoadConfig = LoadConfig(),
    private val onChange: (from: LoadResult?, to: LoadResult) -> Unit = { _, _ -> },
) {
    var current: LoadResult? = null
        private set

    private var pending: LoadResult? = null
    private var pendingSince = 0L

    fun update(signals: VehicleSignals, nowMs: Long): LoadResult {
        val next = engine.evaluate(signals)
        val cur = current
        when {
            cur == null || next.load.level > cur.load.level -> commit(next)
            next.load.level == cur.load.level -> {
                pending = null
                if (next.reason != cur.reason) current = next
            }
            else -> {
                val p = pending
                if (p == null || p.load != next.load) {
                    pending = next
                    pendingSince = nowMs
                } else if (nowMs - pendingSince >= config.calmDownMs) {
                    commit(next)
                } else {
                    pending = next
                }
            }
        }
        return current!!
    }

    private fun commit(next: LoadResult) {
        val prev = current
        current = next
        pending = null
        if (prev?.load != next.load) onChange(prev, next)
    }
}
