package com.burhannexus.dukaandost.ui.products

import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.domain.model.ProductInput
import java.math.BigDecimal
import java.time.LocalDate

enum class FormField { NAME, DESCRIPTION, SKU, BARCODE, PURCHASE_PRICE, SALE_PRICE, STOCK, MIN_STOCK, GENERAL }

enum class FormError { REQUIRED, INVALID_NUMBER, TOO_LONG, DUPLICATE_SKU, DUPLICATE_BARCODE, SAVE_FAILED }

object ProductUnits {
    val ALL = listOf("pcs", "kg", "g", "litre", "ml", "dozen", "packet", "box", "meter")
    const val DEFAULT = "pcs"
}

data class ProductForm(
    val name: String = "",
    val categoryId: Long? = null,
    val description: String = "",
    val sku: String = "",
    val barcode: String = "",
    val purchasePrice: String = "",
    val salePrice: String = "",
    val stock: String = "",
    val minStock: String = "",
    val unit: String = ProductUnits.DEFAULT,
    val expiryDate: LocalDate? = null,
    val errors: Map<FormField, FormError> = emptyMap(),
    val saving: Boolean = false
)

sealed interface ValidationOutcome {
    data class Invalid(val errors: Map<FormField, FormError>) : ValidationOutcome
    data class Valid(val input: ProductInput) : ValidationOutcome
}

object ProductValidator {
    private const val MAX_NAME = 100
    private const val MAX_CODE = 50
    private const val MAX_DESCRIPTION = 500
    private val NUMBER = Regex("""^(\d+\.?\d*|\.\d+)$""")

    /** Parses a non-negative decimal. Returns null when invalid, too precise or too large. */
    fun parseDecimal(text: String, maxScale: Int, max: BigDecimal): BigDecimal? {
        val t = text.trim()
        if (!NUMBER.matches(t)) return null
        val value = BigDecimal(t)
        if (value.stripTrailingZeros().scale() > maxScale) return null
        if (value > max) return null
        return value
    }

    fun validate(id: Long, form: ProductForm): ValidationOutcome {
        val errors = linkedMapOf<FormField, FormError>()

        val name = form.name.trim()
        if (name.isEmpty()) errors[FormField.NAME] = FormError.REQUIRED
        else if (name.length > MAX_NAME) errors[FormField.NAME] = FormError.TOO_LONG

        if (form.description.trim().length > MAX_DESCRIPTION) errors[FormField.DESCRIPTION] = FormError.TOO_LONG
        if (form.sku.trim().length > MAX_CODE) errors[FormField.SKU] = FormError.TOO_LONG
        if (form.barcode.trim().length > MAX_CODE) errors[FormField.BARCODE] = FormError.TOO_LONG

        val purchase = required(form.purchasePrice, 2, Minor.MAX_MONEY, FormField.PURCHASE_PRICE, errors)
        val sale = required(form.salePrice, 2, Minor.MAX_MONEY, FormField.SALE_PRICE, errors)
        val stock = optional(form.stock, 3, Minor.MAX_QUANTITY, FormField.STOCK, errors)
        val minStock = optional(form.minStock, 3, Minor.MAX_QUANTITY, FormField.MIN_STOCK, errors)

        if (errors.isNotEmpty() || purchase == null || sale == null || stock == null || minStock == null) {
            return ValidationOutcome.Invalid(errors)
        }
        return ValidationOutcome.Valid(
            ProductInput(
                id = id,
                name = name,
                categoryId = form.categoryId,
                description = form.description.trim(),
                sku = form.sku.trim().ifEmpty { null },
                barcode = form.barcode.trim().ifEmpty { null },
                purchasePrice = purchase,
                salePrice = sale,
                stock = stock,
                minStock = minStock,
                unit = form.unit,
                expiryDate = form.expiryDate
            )
        )
    }

    private fun required(
        text: String, scale: Int, max: BigDecimal, field: FormField,
        errors: MutableMap<FormField, FormError>
    ): BigDecimal? {
        if (text.isBlank()) {
            errors[field] = FormError.REQUIRED
            return null
        }
        val value = parseDecimal(text, scale, max)
        if (value == null) errors[field] = FormError.INVALID_NUMBER
        return value
    }

    /** Blank means zero. */
    private fun optional(
        text: String, scale: Int, max: BigDecimal, field: FormField,
        errors: MutableMap<FormField, FormError>
    ): BigDecimal? {
        if (text.isBlank()) return BigDecimal.ZERO
        val value = parseDecimal(text, scale, max)
        if (value == null) errors[field] = FormError.INVALID_NUMBER
        return value
    }
}
