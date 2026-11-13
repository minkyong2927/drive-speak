package com.drivespeak.route

import com.drivespeak.load.DrivingLoad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteMemoryTest {
    /** 0~2000m, 1000~1100m(구간 5)에서 합류로 바쁨, 1600m(구간 8)에서 신호 대기 */
    private fun drive(busy: Boolean = true, stopMs: Long = 60_000) = (0..20).map { i ->
        val d = i * 100.0
        DriveSample(
            distanceM = d,
            load = if (busy && (d == 1000.0 || d == 1100.0)) DrivingLoad.BUSY else DrivingLoad.RELAXED,
            stoppedMs = if (d == 1600.0) stopMs else 0,
        )
    }

    private fun memory(drives: Int = 3, busyDrives: Int = drives, stopMs: Long = 60_000) = RouteMemory().apply {
        repeat(drives) { recordDrive(drive(busy = it < busyDrives, stopMs = stopMs)) }
    }

    @Test fun busySegmentWithin30Seconds() =
        assertEquals(BusyAhead(secondsAhead = 20, busyDrives = 3, totalDrives = 3),
            memory().busyAhead(distanceM = 800.0, speedKmh = 36.0))

    @Test fun busySegmentTooFarIsNotReported() =
        assertNull(memory().busyAhead(distanceM = 400.0, speedKmh = 36.0))

    @Test fun noPredictionWhileStopped() =
        assertNull(memory().busyAhead(distanceM = 800.0, speedKmh = 0.0))

    @Test fun needsEnoughDrives() =
        assertNull(memory(drives = 2).busyAhead(distanceM = 800.0, speedKmh = 36.0))

    @Test fun halfOfDrivesBusyIsNotABusySegment() =
        assertNull(memory(drives = 4, busyDrives = 2).busyAhead(distanceM = 800.0, speedKmh = 36.0))

    @Test fun threeOfFourDrivesBusyIsABusySegment() =
        assertEquals(BusyAhead(20, 3, 4), memory(drives = 4, busyDrives = 3).busyAhead(800.0, 36.0))

    @Test fun currentSegmentIsExcluded() =
        assertNull(memory().busyAhead(distanceM = 1050.0, speedKmh = 36.0))

    @Test fun longStopAheadGivesAverage() =
        assertEquals(StopAhead(distanceAheadM = 400.0, averageStopSec = 60),
            memory().longStopAhead(distanceM = 1200.0))

    @Test fun shortStopIsNotReported() =
        assertNull(memory(stopMs = 10_000).longStopAhead(distanceM = 1200.0))

    @Test fun drivesAreCountedPerSegment() {
        assertEquals(3, memory().drivesAt(1000.0))
        assertEquals(0, memory().drivesAt(5000.0))
    }
}
