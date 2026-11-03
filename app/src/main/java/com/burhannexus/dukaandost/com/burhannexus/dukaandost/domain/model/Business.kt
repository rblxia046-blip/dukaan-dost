package com.burhannexus.dukaandost.domain.model

/** The shop's identity. Printed on every receipt. The logo itself is stored as a file. */
data class BusinessProfile(
    val shopName: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val footerNote: String,
    /** 0 = no logo. Changes whenever the logo changes. */
    val logoVersion: Long
) {
    val hasLogo: Boolean get() = logoVersion != 0L
}

data class BusinessProfileInput(
    val shopName: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val footerNote: String
)
