package com.burhannexus.dukaandost

import com.burhannexus.dukaandost.ui.business.BizError
import com.burhannexus.dukaandost.ui.business.BizField
import com.burhannexus.dukaandost.ui.business.BusinessForm
import com.burhannexus.dukaandost.ui.business.BusinessValidation
import com.burhannexus.dukaandost.ui.business.BusinessValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessValidatorTest {
    private val valid = BusinessForm(shopName = " Ali General Store ", phone = "0347-5093446")

    private fun errors(form: BusinessForm) = (BusinessValidator.validate(form) as BusinessValidation.Invalid).errors

    @Test fun validFormIsTrimmed() {
        val input = (BusinessValidator.validate(valid.copy(address = "  Main Bazaar  ")) as BusinessValidation.Valid).input
        assertEquals("Ali General Store", input.shopName)
        assertEquals("Main Bazaar", input.address)
        assertEquals("0347-5093446", input.phone)
    }

    @Test fun addressOwnerAndNoteAreOptional() =
        assertTrue(BusinessValidator.validate(valid) is BusinessValidation.Valid)

    @Test fun shopNameAndPhoneAreRequired() {
        val e = errors(BusinessForm())
        assertEquals(BizError.REQUIRED, e[BizField.SHOP_NAME])
        assertEquals(BizError.REQUIRED, e[BizField.PHONE])
    }

    @Test fun rejectsBadPhoneNumbers() {
        listOf("abc", "12", "0347 509 3446 ext 5", "03475093446<script>").forEach { bad ->
            assertEquals(bad, BizError.INVALID_PHONE, errors(valid.copy(phone = bad))[BizField.PHONE])
        }
    }

    @Test fun acceptsCommonPhoneFormats() {
        listOf("03475093446", "+92 347 5093446", "(042) 111-222-333").forEach { ok ->
            assertTrue(ok, BusinessValidator.validate(valid.copy(phone = ok)) is BusinessValidation.Valid)
        }
    }

    @Test fun limitsLength() {
        assertEquals(BizError.TOO_LONG, errors(valid.copy(shopName = "x".repeat(61)))[BizField.SHOP_NAME])
        assertEquals(BizError.TOO_LONG, errors(valid.copy(address = "x".repeat(201)))[BizField.ADDRESS])
        assertEquals(BizError.TOO_LONG, errors(valid.copy(footerNote = "x".repeat(151)))[BizField.FOOTER_NOTE])
    }
}
