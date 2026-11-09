package com.drivespeak.load

import org.junit.Assert.assertEquals
import org.junit.Test

class LoadTrackerTest {
    private val cruising = VehicleSignals(speedKmh = 62.0)
    private val turning = cruising.copy(turnSignal = TurnSignal.LEFT)

    @Test fun busyIsAppliedImmediately() {
        val changes = mutableListOf<DrivingLoad>()
        val t = LoadTracker(onChange = { _, to -> changes += to.load })
        t.update(cruising, 0)
        assertEquals(DrivingLoad.BUSY, t.update(turning, 100).load)
        assertEquals(listOf(DrivingLoad.RELAXED, DrivingLoad.BUSY), changes)
    }

    @Test fun staysPausedUntilCalmFor1500ms() {
        val t = LoadTracker()
        t.update(turning, 0)
        assertEquals(DrivingLoad.BUSY, t.update(cruising, 1000).load)
        assertEquals(DrivingLoad.BUSY, t.update(cruising, 2000).load)
        assertEquals(DrivingLoad.RELAXED, t.update(cruising, 2500).load)
    }

    @Test fun busyAgainResetsCalmTimer() {
        val t = LoadTracker()
        t.update(turning, 0)
        t.update(cruising, 1000)
        t.update(turning, 2000)
        t.update(cruising, 2200)
        assertEquals(DrivingLoad.BUSY, t.update(cruising, 3600).load)
        assertEquals(DrivingLoad.RELAXED, t.update(cruising, 3700).load)
    }

    @Test fun reasonChangeWithinSameLevelDoesNotNotify() {
        var count = 0
        val t = LoadTracker(onChange = { _, _ -> count++ })
        t.update(turning, 0)
        val r = t.update(cruising.copy(steeringAngleDeg = 40.0), 100)
        assertEquals(LoadReason.LARGE_STEERING, r.reason)
        assertEquals(1, count)
    }
}
