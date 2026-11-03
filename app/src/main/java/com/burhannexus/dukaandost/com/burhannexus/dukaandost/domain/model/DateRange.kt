package com.burhannexus.dukaandost.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class DashboardPeriod { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM }

/** Inclusive date range. */
data class DateRange(val start: LocalDate, val end: LocalDate)

object DateRangeResolver {
    /** Weeks run Monday to Sunday. */
    fun resolve(
        period: DashboardPeriod,
        today: LocalDate,
        customStart: LocalDate? = null,
        customEnd: LocalDate? = null
    ): DateRange = when (period) {
        DashboardPeriod.TODAY -> DateRange(today, today)
        DashboardPeriod.YESTERDAY -> today.minusDays(1).let { DateRange(it, it) }
        DashboardPeriod.THIS_WEEK -> {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            DateRange(start, start.plusDays(6))
        }
        DashboardPeriod.THIS_MONTH ->
            DateRange(today.withDayOfMonth(1), today.with(TemporalAdjusters.lastDayOfMonth()))
        DashboardPeriod.THIS_YEAR ->
            DateRange(today.withDayOfYear(1), today.with(TemporalAdjusters.lastDayOfYear()))
        DashboardPeriod.CUSTOM -> {
            require(customStart != null && customEnd != null) { "Custom range needs both dates" }
            DateRange(minOf(customStart, customEnd), maxOf(customStart, customEnd))
        }
    }
}
