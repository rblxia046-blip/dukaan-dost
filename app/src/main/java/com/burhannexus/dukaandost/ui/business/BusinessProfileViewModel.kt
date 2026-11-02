package com.burhannexus.dukaandost.ui.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.domain.repository.BusinessProfileRepository
import com.burhannexus.dukaandost.ui.trade.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val repository: BusinessProfileRepository
) : ViewModel() {

    private val _form = MutableStateFlow(BusinessForm())
    val form: StateFlow<BusinessForm> = _form.asStateFlow()

    private val _message = MutableStateFlow<UiText?>(null)
    val message: StateFlow<UiText?> = _message.asStateFlow()

    /** PNG bytes of the current logo (null = none). Reloaded whenever the logo changes. */
    val logo: StateFlow<ByteArray?> = repository.logoVersion
        .map { if (it == 0L) null else repository.loadLogoPng() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            // Editing: start from what is saved. First run: stays empty.
            repository.profile.first()?.let { saved ->
                _form.value = BusinessForm(
                    shopName = saved.shopName,
                    ownerName = saved.ownerName,
                    phone = saved.phone,
                    address = saved.address,
                    footerNote = saved.footerNote
                )
            }
        }
    }

    fun edit(field: BizField, block: BusinessForm.() -> BusinessForm) {
        _form.update { current ->
            val updated = current.block()
            updated.copy(errors = updated.errors - field)
        }
    }

    fun setLogo(uri: String) {
        viewModelScope.launch {
            val ok = repository.setLogo(uri)
            _message.value = if (ok) null else UiText(R.string.biz_logo_failed)
        }
    }

    fun removeLogo() {
        viewModelScope.launch {
            repository.removeLogo()
            _message.value = null
        }
    }

    fun save(onSaved: () -> Unit) {
        val current = _form.value
        if (current.saving) return
        when (val result = BusinessValidator.validate(current)) {
            is BusinessValidation.Invalid -> _form.update { it.copy(errors = result.errors) }
            is BusinessValidation.Valid -> {
                _form.update { it.copy(saving = true, errors = emptyMap()) }
                viewModelScope.launch {
                    try {
                        repository.save(result.input)
                        _form.update { it.copy(saving = false) }
                        onSaved()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _form.update { it.copy(saving = false) }
                        _message.value = UiText(R.string.biz_save_failed)
                    }
                }
            }
        }
    }
}
