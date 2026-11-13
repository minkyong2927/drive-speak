package com.drivespeak.agent

import com.drivespeak.load.DrivingLoad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGateTest {
    private fun load(level: Int) = DrivingLoad.values().first { it.level == level }

    @Test fun longQuestionAllowedOnlyWhenStopped() {
        assertEquals(TutorAction.ASK_LONG, SafetyGate.check(TutorAction.ASK_LONG, load(0)).action)
        val cruising = SafetyGate.check(TutorAction.ASK_LONG, load(1))
        assertEquals(TutorAction.ASK_SHORT, cruising.action)
        assertTrue(cruising.lowered)
    }

    @Test fun cautionLevelAllowsListeningOnly() {
        val r = SafetyGate.check(TutorAction.ASK_SHORT, load(2))
        assertEquals(TutorAction.LISTEN_ONLY, r.action)
        assertEquals("DRIVING_LOAD", r.reason)
    }

    @Test fun busyLevelMakesAgentWait() =
        assertEquals(TutorAction.WAIT, SafetyGate.check(TutorAction.ASK_SHORT, load(3)).action)

    @Test fun unstableSteeringLowersToListening() {
        val r = SafetyGate.check(TutorAction.ASK_SHORT, load(1), steeringUnstable = true)
        assertEquals(TutorAction.LISTEN_ONLY, r.action)
        assertEquals("STEERING_UNSTABLE", r.reason)
    }

    @Test fun busySegmentAheadHoldsNewQuestions() {
        val r = SafetyGate.check(TutorAction.ASK_SHORT, load(1), busyAhead = true)
        assertEquals(TutorAction.LISTEN_ONLY, r.action)
        assertEquals("BUSY_AHEAD", r.reason)
    }

    @Test fun gateNeverRaisesWhatAgentChose() {
        val r = SafetyGate.check(TutorAction.LISTEN_ONLY, load(0))
        assertEquals(TutorAction.LISTEN_ONLY, r.action)
        assertFalse(r.lowered)
    }

    @Test fun waitIsAlwaysAllowed() {
        for (l in 0..3) assertFalse(SafetyGate.check(TutorAction.WAIT, load(l), true, true).lowered)
    }
}
