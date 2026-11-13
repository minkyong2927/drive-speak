package com.drivespeak.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceReviewTest {
    private val item = ReviewItem("r1", distanceM = 2600.0, wrong = "my flight is delay",
        correct = "My flight was delayed.", lastDay = 10)

    private fun review() = PlaceReview().apply { add(item) }

    @Test fun dueNextDayAtSamePlace() =
        assertEquals(item, review().due(distanceM = 2650.0, today = 11))

    @Test fun notDueOnTheDayItWasLearned() =
        assertNull(review().due(distanceM = 2650.0, today = 10))

    @Test fun notDueAwayFromThePlace() =
        assertNull(review().due(distanceM = 3000.0, today = 11))

    @Test fun correctAnswerWidensInterval() {
        val r = review()
        r.answered("r1", correct = true, today = 11)
        assertNull(r.due(2600.0, today = 12))
        assertEquals(1, r.due(2600.0, today = 13)?.streak)
    }

    @Test fun wrongAnswerComesBackNextDay() {
        val r = review()
        r.answered("r1", correct = true, today = 11)
        r.answered("r1", correct = false, today = 13)
        assertEquals(0, r.due(2600.0, today = 14)?.streak)
    }

    @Test fun intervalsGrow() =
        assertEquals(listOf(1, 2, 4, 7, 7), (0..4).map { PlaceReview.intervalDays(it) })

    @Test fun unknownIdIsIgnored() {
        val r = review()
        r.answered("nope", correct = true, today = 11)
        assertEquals(listOf(item), r.all())
    }
}
