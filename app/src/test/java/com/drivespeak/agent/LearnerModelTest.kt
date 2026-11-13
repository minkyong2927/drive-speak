package com.drivespeak.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LearnerModelTest {
    private fun model() = LearnerModel().apply {
        record("be동사 누락", 10, "I will late")
        record("be동사 누락", 11, "we going to")
        record("불필요한 전치사", 12, "discuss about")
        record("be동사 누락", 13, "I happy to")
    }

    @Test fun countsByTypeMostFrequentFirst() {
        val w = model().weaknesses(today = 13)
        assertEquals(listOf("be동사 누락", "불필요한 전치사"), w.map { it.type })
        assertEquals(3, w[0].count)
    }

    @Test fun keepsLatestExample() =
        assertEquals("I happy to", model().weaknesses(13)[0].example)

    @Test fun focusNeedsRepeatedErrors() {
        assertEquals("be동사 누락", model().focus(13)?.type)
        assertNull(LearnerModel().apply { record("불필요한 전치사", 12, "discuss about") }.focus(13))
    }

    @Test fun oldErrorsDropOutOfWindow() {
        val m = model()
        assertEquals(1, m.count("be동사 누락", today = 19))
        assertNull(m.focus(19))
    }

    @Test fun blankTypeIsIgnored() =
        assertEquals(0, LearnerModel().apply { record(" ", 1, "x") }.weaknesses(1).size)

    @Test fun tieBreaksByRecency() {
        val m = LearnerModel().apply { record("A", 1, "a"); record("B", 2, "b") }
        assertEquals(listOf("B", "A"), m.weaknesses(2).map { it.type })
    }
}
