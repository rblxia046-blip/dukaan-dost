package com.burhannexus.dukaandost.domain.repository

import com.burhannexus.dukaandost.domain.model.DashboardSummary
import com.burhannexus.dukaandost.domain.model.DateRange
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeSummary(range: DateRange): Flow<DashboardSummary>
}
