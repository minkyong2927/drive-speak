package com.drivespeak.load

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StabilityMonitorTest {
    /** 0.5초마다 ±3°로 흔드는 조향. swings 번 움직임 → 반전은 swings-1 번 */
    private fun wobble(m: StabilityMonitor, swings: Int) {
        m.add(0, 0.0)
        for (i in 1..swings) m.add(i * 500L, if (i % 2 == 1) 3.0 else -3.0)
    }

    @Test fun fiveReversalsIn6SecondsIsUnstable() {
        val m = StabilityMonitor()
        wobble(m, 6)
        assertEquals(5, m.reversals(3000))
        assertTrue(m.isUnstable(3000))
    }

    @Test fun fourReversalsIsStillStable() {
        val m = StabilityMonitor()
        wobble(m, 5)
        assertEquals(4, m.reversals(2500))
        assertFalse(m.isUnstable(2500))
    }

    @Test fun oneWayTurnIsNotWobble() {
        val m = StabilityMonitor()
        listOf(0.0, 10.0, 20.0, 30.0, 40.0).forEachIndexed { i, a -> m.add(i * 300L, a) }
        assertEquals(0, m.reversals(1500))
    }

    @Test fun smallJitterIsIgnored() {
        val m = StabilityMonitor()
        m.add(0, 0.0)
        for (i in 1..20) m.add(i * 200L, if (i % 2 == 1) 0.5 else -0.5)
        assertEquals(0, m.reversals(4000))
    }

    @Test fun oldReversalsExpire() {
        val m = StabilityMonitor()
        wobble(m, 6)
        assertFalse(m.isUnstable(7001))
        assertEquals(0, m.reversals(20_000))
    }

    @Test fun thresholdIsConfigurable() {
        val m = StabilityMonitor(unstableReversals = 2)
        wobble(m, 3)
        assertTrue(m.isUnstable(1500))
    }
}
