package com.burhannexus.dukaandost.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.domain.model.DashboardPeriod
import com.burhannexus.dukaandost.domain.model.DashboardSummary
import com.burhannexus.dukaandost.domain.model.DateRange
import com.burhannexus.dukaandost.domain.model.DateRangeResolver
import com.burhannexus.dukaandost.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class DashboardFilter(
    val period: DashboardPeriod = DashboardPeriod.TODAY,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null
)

data class DashboardUiState(
    val filter: DashboardFilter,
    val range: DateRange,
    val summary: DashboardSummary
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val filter = MutableStateFlow(DashboardFilter())

    val uiState: StateFlow<DashboardUiState> = filter
        .flatMapLatest { f ->
            val range = DateRangeResolver.resolve(f.period, LocalDate.now(), f.customStart, f.customEnd)
            repository.observeSummary(range).map { summary -> DashboardUiState(f, range, summary) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(
                filter = DashboardFilter(),
                range = DateRangeResolver.resolve(DashboardPeriod.TODAY, LocalDate.now()),
                summary = DashboardSummary.EMPTY
            )
        )

    fun selectPeriod(period: DashboardPeriod) {
        filter.value = DashboardFilter(period = period)
    }

    fun selectCustomRange(start: LocalDate, end: LocalDate) {
        filter.value = DashboardFilter(DashboardPeriod.CUSTOM, start, end)
    }
}
