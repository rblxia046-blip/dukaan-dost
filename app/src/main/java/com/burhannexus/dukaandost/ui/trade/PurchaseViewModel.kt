package com.burhannexus.dukaandost.ui.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.domain.calc.PurchaseCalcLine
import com.burhannexus.dukaandost.domain.calc.PurchaseCalculator
import com.burhannexus.dukaandost.domain.model.Party
import com.burhannexus.dukaandost.domain.model.PaymentMethod
import com.burhannexus.dukaandost.domain.model.Product
import com.burhannexus.dukaandost.domain.model.PurchaseInput
import com.burhannexus.dukaandost.domain.model.PurchaseLineInput
import com.burhannexus.dukaandost.domain.model.PurchaseResult
import com.burhannexus.dukaandost.domain.repository.PartyRepository
import com.burhannexus.dukaandost.domain.repository.PurchaseRepository
import com.burhannexus.dukaandost.ui.products.ProductValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class PurchaseLine(
    val product: Product,
    val quantity: BigDecimal,
    val unitCost: BigDecimal,
    val discount: BigDecimal
) {
    val lineTotal: BigDecimal
        get() = quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP).subtract(discount)
}

data class CompletedPurchase(val id: Long, val number: Long)

data class PurchaseUiState(
    val lines: List<PurchaseLine> = emptyList(),
    val suppliers: List<Party> = emptyList(),
    val supplierId: Long? = null,
    val invoiceDiscountText: String = "",
    val extraExpensesText: String = "",
    val paymentMode: PaymentMode = PaymentMode.FULL,
    val paidText: String = "",
    val method: PaymentMethod = PaymentMethod.CASH,
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    val extraExpenses: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,
    val paid: BigDecimal = BigDecimal.ZERO,
    val remaining: BigDecimal = BigDecimal.ZERO,
    val discountError: Boolean = false,
    val extraError: Boolean = false,
    val paidError: Boolean = false,
    val message: UiText? = null,
    val saving: Boolean = false,
    val completed: CompletedPurchase? = null
)

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val purchases: PurchaseRepository,
    private val parties: PartyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseUiState())
    val state: StateFlow<PurchaseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            parties.observeSuppliers().collect { list -> _state.update { it.copy(suppliers = list) } }
        }
    }

    private fun change(block: (PurchaseUiState) -> PurchaseUiState) {
        _state.update { block(it).recomputed() }
    }

    fun addProduct(product: Product) {
        val existing = _state.value.lines.firstOrNull { it.product.id == product.id }
        change { state ->
            val lines = if (existing == null) {
                state.lines + PurchaseLine(product, BigDecimal.ONE, product.purchasePrice, BigDecimal.ZERO)
            } else {
                state.lines.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity.add(BigDecimal.ONE)) else it
                }
            }
            state.copy(lines = lines, message = null)
        }
    }

    fun increase(productId: Long) {
        change { state ->
            state.copy(
                lines = state.lines.map {
                    if (it.product.id == productId) it.copy(quantity = it.quantity.add(BigDecimal.ONE)) else it
                }
            )
        }
    }

    fun decrease(productId: Long) {
        val line = _state.value.lines.firstOrNull { it.product.id == productId } ?: return
        val newQuantity = line.quantity.subtract(BigDecimal.ONE)
        if (newQuantity.signum() <= 0) {
            remove(productId)
        } else {
            change { state ->
                state.copy(
                    lines = state.lines.map {
                        if (it.product.id == productId) {
                            val gross = newQuantity.multiply(it.unitCost).setScale(2, RoundingMode.HALF_UP)
                            it.copy(quantity = newQuantity, discount = it.discount.min(gross))
                        } else it
                    }
                )
            }
        }
    }

    fun remove(productId: Long) {
        change { state -> state.copy(lines = state.lines.filter { it.product.id != productId }) }
    }

    /** Returns an error message, or null when the line was updated. */
    fun updateLine(productId: Long, quantityText: String, priceText: String?, discountText: String): UiText? {
        val line = _state.value.lines.firstOrNull { it.product.id == productId } ?: return null
        val quantity = ProductValidator.parseDecimal(quantityText, 3, Minor.MAX_QUANTITY)
        if (quantity == null || quantity.signum() <= 0) return UiText(R.string.err_invalid_number)
        val unitCost = if (priceText == null) line.unitCost
        else ProductValidator.parseDecimal(priceText, 2, Minor.MAX_MONEY) ?: return UiText(R.string.err_invalid_number)
        val discount = if (discountText.isBlank()) BigDecimal.ZERO
        else ProductValidator.parseDecimal(discountText, 2, Minor.MAX_MONEY) ?: return UiText(R.string.err_invalid_number)
        val gross = quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP)
        if (discount > gross) return UiText(R.string.line_discount_too_high)
        change { state ->
            state.copy(
                lines = state.lines.map {
                    if (it.product.id == productId) it.copy(quantity = quantity, unitCost = unitCost, discount = discount) else it
                }
            )
        }
        return null
    }

    fun selectSupplier(id: Long?) = change { it.copy(supplierId = id, message = null) }
    fun setInvoiceDiscount(text: String) = change { it.copy(invoiceDiscountText = text, message = null) }
    fun setExtraExpenses(text: String) = change { it.copy(extraExpensesText = text, message = null) }
    fun setPaymentMode(mode: PaymentMode) = change { it.copy(paymentMode = mode, message = null) }
    fun setPaidText(text: String) = change { it.copy(paidText = text, message = null) }
    fun setMethod(method: PaymentMethod) = change { it.copy(method = method) }
    fun clearMessage() = change { it.copy(message = null) }
    fun dismissCompleted() = change { it.copy(completed = null) }

    fun addSupplier(name: String, phone: String) {
        viewModelScope.launch {
            val id = parties.addSupplier(name, phone)
            if (id != null) selectSupplier(id)
        }
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        when {
            s.lines.isEmpty() -> return change { it.copy(message = UiText(R.string.purchase_err_empty)) }
            s.discountError || s.extraError || s.paidError ->
                return change { it.copy(message = UiText(R.string.trade_err_amount)) }
            s.remaining.signum() > 0 && s.supplierId == null ->
                return change { it.copy(message = UiText(R.string.purchase_err_supplier_required)) }
        }
        change { it.copy(saving = true, message = null) }
        val input = PurchaseInput(
            supplierId = s.supplierId,
            lines = s.lines.map { PurchaseLineInput(it.product.id, it.quantity, it.unitCost, it.discount) },
            invoiceDiscount = s.discount,
            extraExpenses = s.extraExpenses,
            paid = s.paid,
            method = s.method,
            note = ""
        )
        viewModelScope.launch {
            try {
                when (val result = purchases.save(input)) {
                    is PurchaseResult.Success -> _state.value = PurchaseUiState(
                        suppliers = _state.value.suppliers,
                        completed = CompletedPurchase(result.id, result.number)
                    )
                    PurchaseResult.EmptyCart -> fail(UiText(R.string.purchase_err_empty))
                    PurchaseResult.InvalidAmount -> fail(UiText(R.string.trade_err_amount))
                    PurchaseResult.SupplierRequired -> fail(UiText(R.string.purchase_err_supplier_required))
                    PurchaseResult.ProductMissing -> fail(UiText(R.string.trade_err_product_missing))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(UiText(R.string.trade_err_save_failed))
            }
        }
    }

    private fun fail(message: UiText) = change { it.copy(saving = false, message = message) }
}

private fun parseOptionalMoney(text: String): BigDecimal? {
    val clean = text.trim()
    if (clean.isEmpty()) return BigDecimal.ZERO
    return ProductValidator.parseDecimal(clean, 2, Minor.MAX_MONEY)
}

private fun PurchaseUiState.recomputed(): PurchaseUiState {
    val base = PurchaseCalculator.calculate(
        lines.map { PurchaseCalcLine(it.quantity, it.unitCost, it.discount) },
        BigDecimal.ZERO,
        BigDecimal.ZERO
    )
    val subtotal = base?.subtotal ?: BigDecimal.ZERO

    val parsedDiscount = parseOptionalMoney(invoiceDiscountText)
    val discountError = parsedDiscount == null || parsedDiscount > subtotal
    val discount = if (discountError) BigDecimal.ZERO else parsedDiscount!!

    val parsedExtra = parseOptionalMoney(extraExpensesText)
    val extraError = parsedExtra == null
    val extra = parsedExtra ?: BigDecimal.ZERO

    val total = subtotal.subtract(discount).add(extra)

    var paidError = false
    val paid = when (paymentMode) {
        PaymentMode.FULL -> total
        PaymentMode.CREDIT -> BigDecimal.ZERO
        PaymentMode.PARTIAL -> {
            val parsed = parseOptionalMoney(paidText)
            if (parsed == null || parsed > total) {
                paidError = true
                BigDecimal.ZERO
            } else parsed
        }
    }
    return copy(
        subtotal = subtotal,
        discount = discount,
        extraExpenses = extra,
        total = total,
        paid = paid,
        remaining = total.subtract(paid),
        discountError = discountError,
        extraError = extraError,
        paidError = paidError
    )
}
