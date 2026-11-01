package com.burhannexus.dukaandost

import com.burhannexus.dukaandost.domain.model.DashboardPeriod
import com.burhannexus.dukaandost.domain.model.DateRange
import com.burhannexus.dukaandost.domain.model.DateRangeResolver
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateRangeResolverTest {
    private val sunday = LocalDate.of(2026, 11, 1)

    @Test fun today() = assertEquals(DateRange(sunday, sunday), DateRangeResolver.resolve(DashboardPeriod.TODAY, sunday))

    @Test fun yesterday() {
        val d = LocalDate.of(2026, 10, 31)
        assertEquals(DateRange(d, d), DateRangeResolver.resolve(DashboardPeriod.YESTERDAY, sunday))
    }

    @Test fun weekRunsMondayToSunday() = assertEquals(
        DateRange(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1)),
        DateRangeResolver.resolve(DashboardPeriod.THIS_WEEK, sunday)
    )

    @Test fun month() = assertEquals(
        DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)),
        DateRangeResolver.resolve(DashboardPeriod.THIS_MONTH, sunday)
    )

    @Test fun year() = assertEquals(
        DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
        DateRangeResolver.resolve(DashboardPeriod.THIS_YEAR, sunday)
    )

    @Test fun customRangeIsOrdered() {
        val a = LocalDate.of(2026, 3, 10)
        val b = LocalDate.of(2026, 3, 1)
        assertEquals(DateRange(b, a), DateRangeResolver.resolve(DashboardPeriod.CUSTOM, sunday, a, b))
    }

    @Test(expected = IllegalArgumentException::class)
    fun customWithoutDatesFails() {
        DateRangeResolver.resolve(DashboardPeriod.CUSTOM, sunday)
    }
}
