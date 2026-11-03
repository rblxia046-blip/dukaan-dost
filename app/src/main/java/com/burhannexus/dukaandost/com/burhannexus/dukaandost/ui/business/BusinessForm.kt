package com.burhannexus.dukaandost.ui.business

import com.burhannexus.dukaandost.domain.model.BusinessProfileInput

enum class BizField { SHOP_NAME, OWNER_NAME, PHONE, ADDRESS, FOOTER_NOTE }

enum class BizError { REQUIRED, INVALID_PHONE, TOO_LONG }

data class BusinessForm(
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val footerNote: String = "",
    val errors: Map<BizField, BizError> = emptyMap(),
    val saving: Boolean = false
)

sealed interface BusinessValidation {
    data class Invalid(val errors: Map<BizField, BizError>) : BusinessValidation
    data class Valid(val input: BusinessProfileInput) : BusinessValidation
}

object BusinessValidator {
    private val PHONE = Regex("""^[0-9+\-\s()]{7,20}$""")

    fun validate(form: BusinessForm): BusinessValidation {
        val errors = linkedMapOf<BizField, BizError>()

        val name = form.shopName.trim()
        if (name.isEmpty()) errors[BizField.SHOP_NAME] = BizError.REQUIRED
        else if (name.length > 60) errors[BizField.SHOP_NAME] = BizError.TOO_LONG

        if (form.ownerName.trim().length > 60) errors[BizField.OWNER_NAME] = BizError.TOO_LONG

        val phone = form.phone.trim()
        if (phone.isEmpty()) errors[BizField.PHONE] = BizError.REQUIRED
        else if (!PHONE.matches(phone)) errors[BizField.PHONE] = BizError.INVALID_PHONE

        if (form.address.trim().length > 200) errors[BizField.ADDRESS] = BizError.TOO_LONG
        if (form.footerNote.trim().length > 150) errors[BizField.FOOTER_NOTE] = BizError.TOO_LONG

        if (errors.isNotEmpty()) return BusinessValidation.Invalid(errors)
        return BusinessValidation.Valid(
            BusinessProfileInput(
                shopName = name,
                ownerName = form.ownerName.trim(),
                phone = phone,
                address = form.address.trim(),
                footerNote = form.footerNote.trim()
            )
        )
    }
}
