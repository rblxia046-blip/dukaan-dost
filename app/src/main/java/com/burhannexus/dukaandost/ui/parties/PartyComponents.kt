package com.burhannexus.dukaandost.ui.parties

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.domain.model.PartyAccount
import com.burhannexus.dukaandost.domain.model.PartyInput
import com.burhannexus.dukaandost.domain.model.PartyKind
import com.burhannexus.dukaandost.domain.model.PartySaveResult
import com.burhannexus.dukaandost.ui.products.ProductValidator
import com.burhannexus.dukaandost.ui.trade.DecimalField
import java.math.BigDecimal

/** "To receive Rs. 500", "To pay Rs. 200", "Advance ...", or "Settled". */
@Composable
fun balanceText(kind: PartyKind, balance: BigDecimal): String = when {
    balance.signum() == 0 -> stringResource(R.string.balance_settled)
    balance.signum() > 0 -> stringResource(
        if (kind == PartyKind.CUSTOMER) R.string.balance_to_receive else R.string.balance_to_pay,
        Money.format(balance)
    )
    else -> stringResource(
        if (kind == PartyKind.CUSTOMER) R.string.balance_advance_customer else R.string.balance_advance_supplier,
        Money.format(balance.negate())
    )
}

@Composable
fun balanceColor(kind: PartyKind, balance: BigDecimal) = when {
    balance.signum() == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
    // Money owed TO us is good news, money WE owe is a liability.
    (balance.signum() > 0) == (kind == PartyKind.CUSTOMER) -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyAccountCard(
    account: PartyAccount,
    kind: PartyKind,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                account.phone?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    balanceText(kind, account.balance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = balanceColor(kind, account.balance)
                )
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                }
            }
        }
    }
}

@StringRes
private fun PartySaveResult.messageRes(): Int? = when (this) {
    PartySaveResult.SUCCESS -> null
    PartySaveResult.EMPTY_NAME -> R.string.err_required
    PartySaveResult.TOO_LONG -> R.string.err_too_long
    PartySaveResult.INVALID_AMOUNT -> R.string.err_invalid_number
    PartySaveResult.NOT_FOUND -> R.string.party_not_found
    PartySaveResult.OPENING_LOCKED -> R.string.party_opening_locked
}

/** Add (initial == null) or edit a customer / supplier. */
@Composable
fun PartyFormDialog(
    kind: PartyKind,
    initial: PartyAccount?,
    onDismiss: () -> Unit,
    onSave: (PartyInput, (PartySaveResult) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var opening by remember {
        mutableStateOf(initial?.openingBalance?.takeIf { it.signum() != 0 }?.toPlainString().orEmpty())
    }
    var error by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }

    val titleRes = when {
        initial == null && kind == PartyKind.CUSTOMER -> R.string.customer_add
        initial == null -> R.string.supplier_add
        kind == PartyKind.CUSTOMER -> R.string.customer_edit
        else -> R.string.supplier_edit
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text(stringResource(R.string.party_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.party_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.party_address)) },
                    modifier = Modifier.fillMaxWidth()
                )
                DecimalField(
                    value = opening,
                    onValueChange = { opening = it; error = null },
                    label = stringResource(
                        if (kind == PartyKind.CUSTOMER) R.string.party_opening_customer else R.string.party_opening_supplier
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    val openingValue = if (opening.isBlank()) BigDecimal.ZERO
                    else ProductValidator.parseDecimal(opening, 2, Minor.MAX_MONEY)
                    if (name.isBlank()) {
                        error = R.string.err_required
                    } else if (openingValue == null) {
                        error = R.string.err_invalid_number
                    } else {
                        saving = true
                        onSave(PartyInput(initial?.id ?: 0L, name, phone, address, openingValue)) { result ->
                            saving = false
                            if (result == PartySaveResult.SUCCESS) onDismiss() else error = result.messageRes()
                        }
                    }
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
