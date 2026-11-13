package com.drivespeak.situation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SituationDetectorTest {
    private val onTime = SituationSignals(plannedArrivalMin = 540, expectedArrivalMin = 540, fuelPercent = 34)

    @Test fun nothingOnNormalDrive() =
        assertTrue(SituationDetector().detect(onTime).isEmpty())

    @Test fun lateCarriesMinutes() =
        assertEquals(listOf(SituationEvent(Situation.LATE, 12)),
            SituationDetector().detect(onTime.copy(expectedArrivalMin = 552)))

    @Test fun fourMinutesLateIsNotLate() =
        assertTrue(SituationDetector().detect(onTime.copy(expectedArrivalMin = 544)).isEmpty())

    @Test fun noNavigationMeansNoLate() =
        assertTrue(SituationDetector().detect(SituationSignals(expectedArrivalMin = 600)).isEmpty())

    @Test fun sameSituationOnlyOncePerDrive() {
        val d = SituationDetector()
        val late = onTime.copy(expectedArrivalMin = 552)
        assertEquals(1, d.detect(late).size)
        assertTrue(d.detect(late.copy(expectedArrivalMin = 560)).isEmpty())
    }

    @Test fun resetAllowsAgainOnNextDrive() {
        val d = SituationDetector()
        val late = onTime.copy(expectedArrivalMin = 552)
        d.detect(late)
        d.reset()
        assertEquals(1, d.detect(late).size)
    }

    @Test fun rainValueByWiperSpeed() {
        assertEquals(listOf(SituationEvent(Situation.RAIN, 1)), SituationDetector().detect(onTime.copy(wiper = Wiper.LOW)))
        assertEquals(listOf(SituationEvent(Situation.RAIN, 2)), SituationDetector().detect(onTime.copy(wiper = Wiper.HIGH)))
    }

    @Test fun lowFuelAt15PercentCarriesPercent() {
        assertEquals(listOf(SituationEvent(Situation.LOW_FUEL, 15)), SituationDetector().detect(onTime.copy(fuelPercent = 15)))
        assertTrue(SituationDetector().detect(onTime.copy(fuelPercent = 16)).isEmpty())
    }

    @Test fun passengerAndParking() {
        val got = SituationDetector().detect(onTime.copy(passengerSeated = true, place = PlaceType.PARKING))
        assertEquals(listOf(Situation.PASSENGER, Situation.PARKING), got.map { it.situation })
    }

    @Test fun severalAtOnceInPriorityOrder() {
        val got = SituationDetector().detect(onTime.copy(expectedArrivalMin = 560, wiper = Wiper.HIGH, fuelPercent = 10))
        assertEquals(listOf(Situation.LATE, Situation.RAIN, Situation.LOW_FUEL), got.map { it.situation })
    }

    @Test fun thresholdsAreConfigurable() {
        val d = SituationDetector(SituationConfig(lateMinutes = 3, lowFuelPercent = 20))
        val got = d.detect(onTime.copy(expectedArrivalMin = 543, fuelPercent = 20))
        assertEquals(listOf(Situation.LATE, Situation.LOW_FUEL), got.map { it.situation })
    }
}
