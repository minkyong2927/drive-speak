package com.drivespeak.load

import org.junit.Assert.assertEquals
import org.junit.Test

class LoadEngineTest {
    private val engine = LoadEngine()
    private val cruising = VehicleSignals(speedKmh = 62.0)

    private fun check(s: VehicleSignals, load: DrivingLoad, reason: LoadReason) {
        assertEquals(LoadResult(load, reason), engine.evaluate(s))
    }

    @Test fun cruisingIsRelaxed() = check(cruising, DrivingLoad.RELAXED, LoadReason.CRUISING)

    @Test fun stoppedIsLevel0() =
        check(cruising.copy(speedKmh = 0.0), DrivingLoad.STOPPED, LoadReason.STOPPED)

    @Test fun parkedWinsOverOtherSignals() =
        check(cruising.copy(parked = true, turnSignal = TurnSignal.LEFT), DrivingLoad.STOPPED, LoadReason.PARKED)

    @Test fun turnSignalIsBusy() =
        check(cruising.copy(turnSignal = TurnSignal.RIGHT), DrivingLoad.BUSY, LoadReason.TURN_SIGNAL)

    @Test fun turnSignalWhileStoppedIsBusy() =
        check(cruising.copy(speedKmh = 0.0, turnSignal = TurnSignal.LEFT), DrivingLoad.BUSY, LoadReason.TURN_SIGNAL)

    @Test fun steering30DegOrMoreIsBusyBothSides() {
        check(cruising.copy(steeringAngleDeg = 30.0), DrivingLoad.BUSY, LoadReason.LARGE_STEERING)
        check(cruising.copy(steeringAngleDeg = -42.0), DrivingLoad.BUSY, LoadReason.LARGE_STEERING)
    }

    @Test fun steering29DegIsRelaxed() =
        check(cruising.copy(steeringAngleDeg = 29.0), DrivingLoad.RELAXED, LoadReason.CRUISING)

    @Test fun hardBrakingIsBusy() =
        check(cruising.copy(decelerationMps2 = 5.0), DrivingLoad.BUSY, LoadReason.HARD_BRAKING)

    @Test fun navVoiceIsBusy() =
        check(cruising.copy(navGuidance = NavGuidance.VOICE), DrivingLoad.BUSY, LoadReason.NAV_VOICE)

    @Test fun turnWithin300mIsModerate() =
        check(cruising.copy(navGuidance = NavGuidance.TURN_AHEAD, navDistanceM = 300.0),
            DrivingLoad.MODERATE, LoadReason.NAV_TURN_NEAR)

    @Test fun turnAt301mIsRelaxed() =
        check(cruising.copy(navGuidance = NavGuidance.TURN_AHEAD, navDistanceM = 301.0),
            DrivingLoad.RELAXED, LoadReason.CRUISING)

    @Test fun wiperHighIsModerate() =
        check(cruising.copy(wiperHigh = true), DrivingLoad.MODERATE, LoadReason.WIPER_HIGH)

    @Test fun thresholdsAreConfigurable() {
        val strict = LoadEngine(LoadConfig(largeSteeringDeg = 20.0))
        assertEquals(DrivingLoad.BUSY, strict.evaluate(cruising.copy(steeringAngleDeg = 25.0)).load)
    }

    @Test fun tutorModePerLevel() {
        assertEquals(TutorMode.FREE_TALK, DrivingLoad.STOPPED.tutorMode)
        assertEquals(TutorMode.SHORT_QUESTION, DrivingLoad.RELAXED.tutorMode)
        assertEquals(TutorMode.LISTEN_ONLY, DrivingLoad.MODERATE.tutorMode)
        assertEquals(TutorMode.PAUSE, DrivingLoad.BUSY.tutorMode)
    }
}
