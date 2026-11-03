package com.burhannexus.dukaandost.ui.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.domain.calc.SaleCalcLine
import com.burhannexus.dukaandost.domain.calc.SaleCalculator
import com.burhannexus.dukaandost.domain.model.Party
import com.burhannexus.dukaandost.domain.model.PaymentMethod
import com.burhannexus.dukaandost.domain.model.Product
import com.burhannexus.dukaandost.domain.model.SaleInput
import com.burhannexus.dukaandost.domain.model.SaleLineInput
import com.burhannexus.dukaandost.domain.model.SaleResult
import com.burhannexus.dukaandost.domain.repository.PartyRepository
import com.burhannexus.dukaandost.domain.repository.SalesRepository
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
import java.util.UUID
import javax.inject.Inject

data class SaleLine(val product: Product, val quantity: BigDecimal, val discount: BigDecimal) {
    val unitPrice: BigDecimal get() = product.salePrice
    val lineTotal: BigDecimal
        get() = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP).subtract(discount)
}

data class CompletedSale(val id: Long, val number: Long)

data class PosUiState(
    val lines: List<SaleLine> = emptyList(),
    val customers: List<Party> = emptyList(),
    val customerId: Long? = null,
    val invoiceDiscountText: String = "",
    val paymentMode: PaymentMode = PaymentMode.FULL,
    val paidText: String = "",
    val method: PaymentMethod = PaymentMethod.CASH,
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,
    val paid: BigDecimal = BigDecimal.ZERO,
    val remaining: BigDecimal = BigDecimal.ZERO,
    val discountError: Boolean = false,
    val paidError: Boolean = false,
    val message: UiText? = null,
    val saving: Boolean = false,
    val completed: CompletedSale? = null
)

@HiltViewModel
class PosViewModel @Inject constructor(
    private val sales: SalesRepository,
    private val parties: PartyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PosUiState())
    val state: StateFlow<PosUiState> = _state.asStateFlow()

    /** One token per checkout attempt: saving it twice can never create two sales. */
    private var token: String = UUID.randomUUID().toString()

    init {
        viewModelScope.launch {
            parties.observeCustomers().collect { list -> _state.update { it.copy(customers = list) } }
        }
    }

    private fun change(block: (PosUiState) -> PosUiState) {
        _state.update { block(it).recomputed() }
    }

    fun addProduct(product: Product) {
        val current = _state.value.lines.firstOrNull { it.product.id == product.id }
        if (product.stock.signum() <= 0) {
            change { it.copy(message = UiText(R.string.pos_err_out_of_stock, listOf(product.name))) }
            return
        }
        val newQuantity = if (current == null) BigDecimal.ONE.min(product.stock) else current.quantity.add(BigDecimal.ONE)
        if (newQuantity > product.stock) {
            change { it.copy(message = onlyInStock(product)) }
            return
        }
        change { state ->
            val lines = if (current == null) {
                state.lines + SaleLine(product, newQuantity, BigDecimal.ZERO)
            } else {
                state.lines.map { if (it.product.id == product.id) it.copy(quantity = newQuantity) else it }
            }
            state.copy(lines = lines, message = null)
        }
    }

    fun increase(productId: Long) {
        val line = _state.value.lines.firstOrNull { it.product.id == productId } ?: return
        addProduct(line.product)
    }

    fun decrease(productId: Long) {
        val line = _state.value.lines.firstOrNull { it.product.id == productId } ?: return
        val newQuantity = line.quantity.subtract(BigDecimal.ONE)
        if (newQuantity.signum() <= 0) remove(productId)
        else change { state ->
            state.copy(
                lines = state.lines.map {
                    if (it.product.id == productId) it.copy(quantity = newQuantity, discount = clampDiscount(it, newQuantity)) else it
                },
                message = null
            )
        }
    }

    fun remove(productId: Long) {
        change { state -> state.copy(lines = state.lines.filter { it.product.id != productId }, message = null) }
    }

    /** Returns an error message, or null when the line was updated. */
    fun updateLine(productId: Long, quantityText: String, discountText: String): UiText? {
        val line = _state.value.lines.firstOrNull { it.product.id == productId } ?: return null
        val quantity = ProductValidator.parseDecimal(quantityText, 3, Minor.MAX_QUANTITY)
        if (quantity == null || quantity.signum() <= 0) return UiText(R.string.err_invalid_number)
        if (quantity > line.product.stock) return onlyInStock(line.product)
        val discount = if (discountText.isBlank()) BigDecimal.ZERO
        else ProductValidator.parseDecimal(discountText, 2, Minor.MAX_MONEY) ?: return UiText(R.string.err_invalid_number)
        val gross = quantity.multiply(line.unitPrice).setScale(2, RoundingMode.HALF_UP)
        if (discount > gross) return UiText(R.string.line_discount_too_high)
        change { state ->
            state.copy(
                lines = state.lines.map {
                    if (it.product.id == productId) it.copy(quantity = quantity, discount = discount) else it
                },
                message = null
            )
        }
        return null
    }

    fun selectCustomer(id: Long?) = change { it.copy(customerId = id, message = null) }
    fun setInvoiceDiscount(text: String) = change { it.copy(invoiceDiscountText = text, message = null) }
    fun setPaymentMode(mode: PaymentMode) = change { it.copy(paymentMode = mode, message = null) }
    fun setPaidText(text: String) = change { it.copy(paidText = text, message = null) }
    fun setMethod(method: PaymentMethod) = change { it.copy(method = method) }
    fun clearMessage() = change { it.copy(message = null) }
    fun dismissCompleted() = change { it.copy(completed = null) }

    fun addCustomer(name: String, phone: String) {
        viewModelScope.launch {
            val id = parties.addCustomer(name, phone)
            if (id != null) selectCustomer(id)
        }
    }

    fun checkout() {
        val s = _state.value
        if (s.saving) return
        when {
            s.lines.isEmpty() -> return change { it.copy(message = UiText(R.string.pos_err_empty)) }
            s.discountError || s.paidError -> return change { it.copy(message = UiText(R.string.trade_err_amount)) }
            s.remaining.signum() > 0 && s.customerId == null ->
                return change { it.copy(message = UiText(R.string.pos_err_customer_required)) }
        }
        change { it.copy(saving = true, message = null) }
        val input = SaleInput(
            clientToken = token,
            customerId = s.customerId,
            lines = s.lines.map { SaleLineInput(it.product.id, it.quantity, it.unitPrice, it.discount) },
            invoiceDiscount = s.discount,
            paid = s.paid,
            method = s.method,
            note = ""
        )
        viewModelScope.launch {
            try {
                when (val result = sales.save(input)) {
                    is SaleResult.Success -> {
                        token = UUID.randomUUID().toString()
                        _state.value = PosUiState(
                            customers = _state.value.customers,
                            completed = CompletedSale(result.id, result.number)
                        )
                    }
                    is SaleResult.InsufficientStock -> fail(
                        UiText(
                            R.string.pos_err_stock,
                            listOf(result.productName, Money.formatQuantity(result.available))
                        )
                    )
                    SaleResult.EmptyCart -> fail(UiText(R.string.pos_err_empty))
                    SaleResult.InvalidAmount -> fail(UiText(R.string.trade_err_amount))
                    SaleResult.CustomerRequired -> fail(UiText(R.string.pos_err_customer_required))
                    SaleResult.ProductMissing -> fail(UiText(R.string.trade_err_product_missing))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(UiText(R.string.trade_err_save_failed))
            }
        }
    }

    private fun fail(message: UiText) = change { it.copy(saving = false, message = message) }

    private fun onlyInStock(product: Product) =
        UiText(R.string.pos_only_in_stock, listOf(Money.formatQuantity(product.stock), product.unit, product.name))

    private fun clampDiscount(line: SaleLine, newQuantity: BigDecimal): BigDecimal {
        val gross = newQuantity.multiply(line.unitPrice).setScale(2, RoundingMode.HALF_UP)
        return line.discount.min(gross)
    }

}

private fun PosUiState.recomputed(): PosUiState {
    val base = SaleCalculator.calculate(
        lines.map { SaleCalcLine(it.quantity, it.unitPrice, it.discount) },
        BigDecimal.ZERO
    )
    val subtotal = base?.subtotal ?: BigDecimal.ZERO

    val discountInput = invoiceDiscountText.trim()
    var discountError = false
    val discount = if (discountInput.isEmpty()) {
        BigDecimal.ZERO
    } else {
        val parsed = ProductValidator.parseDecimal(discountInput, 2, Minor.MAX_MONEY)
        if (parsed == null || parsed > subtotal) {
            discountError = true
            BigDecimal.ZERO
        } else parsed
    }
    val total = subtotal.subtract(discount)

    var paidError = false
    val paid = when (paymentMode) {
        PaymentMode.FULL -> total
        PaymentMode.CREDIT -> BigDecimal.ZERO
        PaymentMode.PARTIAL -> {
            val text = paidText.trim()
            if (text.isEmpty()) {
                BigDecimal.ZERO
            } else {
                val parsed = ProductValidator.parseDecimal(text, 2, Minor.MAX_MONEY)
                if (parsed == null || parsed > total) {
                    paidError = true
                    BigDecimal.ZERO
                } else parsed
            }
        }
    }
    return copy(
        subtotal = subtotal,
        discount = discount,
        total = total,
        paid = paid,
        remaining = total.subtract(paid),
        discountError = discountError,
        paidError = paidError
    )
}
