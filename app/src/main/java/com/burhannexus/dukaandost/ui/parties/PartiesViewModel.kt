package com.burhannexus.dukaandost.ui.parties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.domain.model.PartyAccount
import com.burhannexus.dukaandost.domain.model.PartyInput
import com.burhannexus.dukaandost.domain.model.PartyKind
import com.burhannexus.dukaandost.domain.model.PartySaveResult
import com.burhannexus.dukaandost.domain.repository.PartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shared by the Customers, Suppliers and Ledger screens. Call [start] with the kind to show. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PartiesViewModel @Inject constructor(
    private val repository: PartyRepository
) : ViewModel() {

    private val kind = MutableStateFlow<PartyKind?>(null)
    private val query = MutableStateFlow("")

    val accounts: StateFlow<List<PartyAccount>> = combine(
        kind.filterNotNull().flatMapLatest { repository.observeAccounts(it) },
        query
    ) { list, q ->
        val text = q.trim()
        if (text.isEmpty()) list
        else list.filter { it.name.contains(text, ignoreCase = true) || (it.phone ?: "").contains(text) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun start(value: PartyKind) {
        kind.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun save(kind: PartyKind, input: PartyInput, onResult: (PartySaveResult) -> Unit) {
        viewModelScope.launch { onResult(repository.saveParty(kind, input)) }
    }
}
