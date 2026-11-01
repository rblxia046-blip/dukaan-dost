package com.burhannexus.dukaandost.ui.trade

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.domain.model.Party
import com.burhannexus.dukaandost.domain.model.PaymentMethod
import com.burhannexus.dukaandost.domain.model.Product
import java.math.BigDecimal

@Composable
fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

/** Searchable list of active products. Typing a barcode or SKU works too. */
@Composable
fun ProductPickerDialog(
    onPick: (Product) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ProductPickerViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.setQuery("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_product_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.setQuery(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.products_search_hint)) }
                )
                Spacer(Modifier.size(8.dp))
                if (products.isEmpty()) {
                    Text(
                        stringResource(R.string.pick_product_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(products, key = { it.id }) { product ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(product) }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                                    Text(
                                        stringResource(
                                            R.string.pick_product_stock,
                                            Money.formatQuantity(product.stock),
                                            product.unit
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(Money.format(product.salePrice), style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

/** Supplier / customer chooser with quick-add. Full management comes in Phase 4. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartySelector(
    label: String,
    parties: List<Party>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onAdd: (name: String, phone: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val noneText = stringResource(R.string.party_none)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = parties.firstOrNull { it.id == selectedId }?.name ?: noneText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noneText) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            parties.forEach { party ->
                DropdownMenuItem(
                    text = { Text(party.name) },
                    onClick = {
                        onSelect(party.id)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.party_add_new)) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    showAdd = true
                }
            )
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.party_add_new)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.party_name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.party_phone)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onAdd(name, phone)
                        showAdd = false
                    }
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
fun PaymentSection(
    mode: PaymentMode,
    onModeChange: (PaymentMode) -> Unit,
    paidText: String,
    onPaidTextChange: (String) -> Unit,
    paidError: Boolean,
    method: PaymentMethod,
    onMethodChange: (PaymentMethod) -> Unit,
    paid: BigDecimal,
    remaining: BigDecimal
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == PaymentMode.FULL,
                onClick = { onModeChange(PaymentMode.FULL) },
                label = { Text(stringResource(R.string.pay_full)) }
            )
            FilterChip(
                selected = mode == PaymentMode.PARTIAL,
                onClick = { onModeChange(PaymentMode.PARTIAL) },
                label = { Text(stringResource(R.string.pay_partial)) }
            )
            FilterChip(
                selected = mode == PaymentMode.CREDIT,
                onClick = { onModeChange(PaymentMode.CREDIT) },
                label = { Text(stringResource(R.string.pay_credit)) }
            )
        }
        if (mode == PaymentMode.PARTIAL) {
            DecimalField(
                value = paidText,
                onValueChange = onPaidTextChange,
                label = stringResource(R.string.pay_amount_paid),
                modifier = Modifier.fillMaxWidth(),
                isError = paidError
            )
            if (paidError) {
                Text(
                    stringResource(R.string.pay_amount_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (paid.signum() > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.entries.forEach { option ->
                    FilterChip(
                        selected = method == option,
                        onClick = { onMethodChange(option) },
                        label = { Text(stringResource(option.labelRes())) }
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.pay_remaining), style = MaterialTheme.typography.bodyMedium)
            Text(Money.format(remaining), style = MaterialTheme.typography.titleMedium)
        }
    }
}

fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.CASH -> R.string.method_cash
    PaymentMethod.BANK -> R.string.method_bank
    PaymentMethod.WALLET -> R.string.method_wallet
}

@Composable
fun CartLineRow(
    name: String,
    detail: String,
    total: String,
    quantityText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2)
                Text(total, style = MaterialTheme.typography.titleMedium)
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMinus) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.cart_decrease))
                }
                Text(quantityText, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onPlus) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cart_increase))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cart_remove))
                }
            }
        }
    }
}

/** Edit quantity, discount and (for purchases) the unit price of one cart line. */
@Composable
fun LineEditDialog(
    title: String,
    initialQuantity: String,
    initialPrice: String?,
    initialDiscount: String,
    onConfirm: (quantity: String, price: String?, discount: String) -> UiText?,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf(initialQuantity) }
    var price by remember { mutableStateOf(initialPrice.orEmpty()) }
    var discount by remember { mutableStateOf(initialDiscount) }
    var error by remember { mutableStateOf<UiText?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(quantity, { quantity = it }, stringResource(R.string.line_quantity), Modifier.fillMaxWidth())
                if (initialPrice != null) {
                    DecimalField(price, { price = it }, stringResource(R.string.line_unit_cost), Modifier.fillMaxWidth())
                }
                DecimalField(discount, { discount = it }, stringResource(R.string.line_discount), Modifier.fillMaxWidth())
                error?.let {
                    Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = onConfirm(quantity, if (initialPrice != null) price else null, discount)
                if (result == null) onDismiss() else error = result
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
