package com.burhannexus.dukaandost.ui.settings

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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repository: BusinessProfileRepository
) : ViewModel() {
    val profile: StateFlow<BusinessProfile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val logo: StateFlow<ByteArray?> = repository.logoVersion
        .map { if (it == 0L) null else repository.loadLogoPng() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
