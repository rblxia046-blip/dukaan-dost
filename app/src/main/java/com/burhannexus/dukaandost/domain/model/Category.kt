package com.burhannexus.dukaandost.domain.model

data class Category(val id: Long, val name: String, val productCount: Int)

enum class CategoryResult { SUCCESS, EMPTY_NAME, TOO_LONG, DUPLICATE_NAME, NOT_FOUND }
