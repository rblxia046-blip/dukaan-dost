package com.burhannexus.dukaandost.ui.trade

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** A message made of a string resource plus arguments; resolved in the UI layer. */
data class UiText(@StringRes val resId: Int, val args: List<Any> = emptyList())

@Composable
fun UiText.asString(): String = stringResource(resId, *args.toTypedArray())

enum class PaymentMode { FULL, PARTIAL, CREDIT }
