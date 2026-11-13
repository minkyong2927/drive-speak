package com.drivespeak.pleos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PleosMapperTest {
    @Test fun turnSignalNames() {
        assertEquals("left", PleosMapper.turn("LEFT"))
        assertEquals("right", PleosMapper.turn("RIGHT"))
        assertEquals("off", PleosMapper.turn("OFF"))
        assertEquals("off", PleosMapper.turn(null))
    }

    @Test fun wiperOffWhenStateIsNotOn() {
        assertEquals("off", PleosMapper.wiper("OFF", "CONTINUOUS_LEVEL_3"))
        assertEquals("off", PleosMapper.wiper(null, null))
    }

    @Test fun wiperHighFromContinuousLevel2() {
        assertEquals("low", PleosMapper.wiper("ON", "CONTINUOUS_LEVEL_1"))
        assertEquals("high", PleosMapper.wiper("ON", "CONTINUOUS_LEVEL_2"))
        assertEquals("high", PleosMapper.wiper("ON", "CONTINUOUS_LEVEL_5"))
    }

    @Test fun wiperLowWhenSwitchUnknownOrIntermittent() {
        assertEquals("low", PleosMapper.wiper("ON", null))
        assertEquals("low", PleosMapper.wiper("ON", "INTERMITTENT_LEVEL_4"))
        assertEquals("low", PleosMapper.wiper("ON", "AUTO"))
    }

    @Test fun parkedOnlyInParkGear() {
        assertTrue(PleosMapper.parked("GEAR_PARK"))
        assertFalse(PleosMapper.parked("GEAR_DRIVE"))
        assertFalse(PleosMapper.parked(null))
    }

    @Test fun turnNearWithin300m() {
        assertTrue(PleosMapper.turnNear(300.0))
        assertFalse(PleosMapper.turnNear(301.0))
        assertFalse(PleosMapper.turnNear(null))
    }

    @Test fun delayIsLateMinutesAgainstPlannedArrival() {
        val planned = 3_600_000L                       // 출발 때 정해진 도착 시각
        assertEquals(12, PleosMapper.delayMin(planned, nowMs = 1_800_000L, remainSec = 42 * 60))
    }

    @Test fun noDelayWhenEarlyOrUnknown() {
        assertEquals(0, PleosMapper.delayMin(3_600_000L, nowMs = 1_800_000L, remainSec = 20 * 60))
        assertEquals(0, PleosMapper.delayMin(null, nowMs = 0L, remainSec = 600))
        assertEquals(0, PleosMapper.delayMin(3_600_000L, nowMs = 0L, remainSec = null))
    }

    @Test fun hhmmFormat() {
        assertEquals("09:00", PleosMapper.hhmm(540))
        assertEquals("00:05", PleosMapper.hhmm(24 * 60 + 5))
    }
}
