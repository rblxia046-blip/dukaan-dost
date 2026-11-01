package com.burhannexus.dukaandost

import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.ui.products.FormError
import com.burhannexus.dukaandost.ui.products.FormField
import com.burhannexus.dukaandost.ui.products.ProductForm
import com.burhannexus.dukaandost.ui.products.ProductValidator
import com.burhannexus.dukaandost.ui.products.ValidationOutcome
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductValidatorTest {
    private val valid = ProductForm(name = "Mobile Charger", purchasePrice = "125", salePrice = "150.50")

    private fun errorsOf(form: ProductForm) =
        (ProductValidator.validate(0, form) as ValidationOutcome.Invalid).errors

    @Test fun validFormProducesInput() {
        val outcome = ProductValidator.validate(0, valid.copy(stock = "10", minStock = "2.5", sku = " A1 "))
        val input = (outcome as ValidationOutcome.Valid).input
        assertEquals("Mobile Charger", input.name)
        assertEquals(BigDecimal("125"), input.purchasePrice)
        assertEquals(BigDecimal("150.50"), input.salePrice)
        assertEquals(BigDecimal("10"), input.stock)
        assertEquals(BigDecimal("2.5"), input.minStock)
        assertEquals("A1", input.sku)
        assertNull(input.barcode)
    }

    @Test fun blankStockMeansZero() {
        val input = (ProductValidator.validate(0, valid) as ValidationOutcome.Valid).input
        assertEquals(0, input.stock.signum())
        assertEquals(0, input.minStock.signum())
    }

    @Test fun nameIsRequired() =
        assertEquals(FormError.REQUIRED, errorsOf(valid.copy(name = "  "))[FormField.NAME])

    @Test fun nameTooLong() =
        assertEquals(FormError.TOO_LONG, errorsOf(valid.copy(name = "x".repeat(101)))[FormField.NAME])

    @Test fun pricesAreRequired() {
        val errors = errorsOf(valid.copy(purchasePrice = "", salePrice = ""))
        assertEquals(FormError.REQUIRED, errors[FormField.PURCHASE_PRICE])
        assertEquals(FormError.REQUIRED, errors[FormField.SALE_PRICE])
    }

    @Test fun rejectsNonNumbersNegativesAndTooManyDecimals() {
        listOf("abc", "-5", "1.234", "1,5", "1e3", "--").forEach { bad ->
            assertEquals(
                "price '$bad'",
                FormError.INVALID_NUMBER,
                errorsOf(valid.copy(purchasePrice = bad))[FormField.PURCHASE_PRICE]
            )
        }
    }

    @Test fun stockAllowsThreeDecimalsButNotFour() {
        assertTrue(ProductValidator.validate(0, valid.copy(stock = "1.125")) is ValidationOutcome.Valid)
        assertEquals(FormError.INVALID_NUMBER, errorsOf(valid.copy(stock = "1.1234"))[FormField.STOCK])
    }

    @Test fun rejectsValuesAboveMaximum() {
        assertEquals(FormError.INVALID_NUMBER, errorsOf(valid.copy(purchasePrice = "1000000000"))[FormField.PURCHASE_PRICE])
        assertTrue(ProductValidator.validate(0, valid.copy(purchasePrice = "999999999.99")) is ValidationOutcome.Valid)
    }

    @Test fun parseDecimalAcceptsLeadingAndTrailingDot() {
        assertEquals(BigDecimal(".5"), ProductValidator.parseDecimal(".5", 2, Minor.MAX_MONEY))
        assertEquals(BigDecimal("5."), ProductValidator.parseDecimal("5.", 2, Minor.MAX_MONEY))
    }

    @Test fun trailingZerosDoNotCountAsPrecision() =
        assertEquals(BigDecimal("1.500"), ProductValidator.parseDecimal("1.500", 2, Minor.MAX_MONEY))
}
