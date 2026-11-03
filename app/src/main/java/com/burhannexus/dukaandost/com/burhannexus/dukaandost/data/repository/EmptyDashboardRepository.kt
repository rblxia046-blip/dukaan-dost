package com.burhannexus.dukaandost.data.repository

import com.burhannexus.dukaandost.domain.model.DashboardSummary
import com.burhannexus.dukaandost.domain.model.DateRange
import com.burhannexus.dukaandost.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1 only: no database exists yet, so there are truly no records and the
 * dashboard shows empty values. Phase 2 replaces this with a Room-backed
 * implementation (see di/AppModule.kt).
 */
@Singleton
class EmptyDashboardRepository @Inject constructor() : DashboardRepository {
    override fun observeSummary(range: DateRange): Flow<DashboardSummary> = flowOf(DashboardSummary.EMPTY)
}
