package com.burhannexus.dukaandost.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.domain.model.BusinessProfile
import com.burhannexus.dukaandost.domain.repository.BusinessProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SetupState {
    object Loading : SetupState
    /** First run: the shop details have not been entered yet. */
    object Needed : SetupState
    data class Done(val profile: BusinessProfile) : SetupState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    repository: BusinessProfileRepository
) : ViewModel() {
    val setup: StateFlow<SetupState> = repository.profile
        .map<BusinessProfile?, SetupState> { if (it == null) SetupState.Needed else SetupState.Done(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetupState.Loading)
}
