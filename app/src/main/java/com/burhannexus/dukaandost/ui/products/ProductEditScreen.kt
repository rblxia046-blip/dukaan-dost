package com.burhannexus.dukaandost.ui.products

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@StringRes
private fun FormError.messageRes(): Int = when (this) {
    FormError.REQUIRED -> R.string.err_required
    FormError.INVALID_NUMBER -> R.string.err_invalid_number
    FormError.TOO_LONG -> R.string.err_too_long
    FormError.DUPLICATE_SKU -> R.string.err_duplicate_sku
    FormError.DUPLICATE_BARCODE -> R.string.err_duplicate_barcode
    FormError.SAVE_FAILED -> R.string.err_save_failed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    viewModel: ProductEditViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    when (loadState) {
        LoadState.LOADING -> {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        LoadState.NOT_FOUND -> {
            Column(
                Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.product_not_found), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onClose) { Text(stringResource(R.string.go_back)) }
            }
            return
        }
        LoadState.READY -> Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(if (viewModel.isNew) R.string.product_add_title else R.string.product_edit_title),
            style = MaterialTheme.typography.titleLarge
        )

        FormTextField(
            value = form.name,
            onValueChange = { v -> viewModel.edit(FormField.NAME) { copy(name = v) } },
            label = stringResource(R.string.field_name),
            error = form.errors[FormField.NAME]
        )

        DropdownField(
            label = stringResource(R.string.field_category),
            valueText = categories.firstOrNull { it.id == form.categoryId }?.name
                ?: stringResource(R.string.category_none),
            options = listOf<Pair<Long?, String>>(null to stringResource(R.string.category_none)) +
                categories.map { it.id to it.name },
            onSelect = { id -> viewModel.edit { copy(categoryId = id) } }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormTextField(
                value = form.purchasePrice,
                onValueChange = { v -> viewModel.edit(FormField.PURCHASE_PRICE) { copy(purchasePrice = v) } },
                label = stringResource(R.string.field_purchase_price),
                error = form.errors[FormField.PURCHASE_PRICE],
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            FormTextField(
                value = form.salePrice,
                onValueChange = { v -> viewModel.edit(FormField.SALE_PRICE) { copy(salePrice = v) } },
                label = stringResource(R.string.field_sale_price),
                error = form.errors[FormField.SALE_PRICE],
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormTextField(
                value = form.stock,
                onValueChange = { v -> viewModel.edit(FormField.STOCK) { copy(stock = v) } },
                label = stringResource(
                    if (viewModel.isNew) R.string.field_opening_stock else R.string.field_current_stock
                ),
                error = form.errors[FormField.STOCK],
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            FormTextField(
                value = form.minStock,
                onValueChange = { v -> viewModel.edit(FormField.MIN_STOCK) { copy(minStock = v) } },
                label = stringResource(R.string.field_min_stock),
                error = form.errors[FormField.MIN_STOCK],
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }
        if (!viewModel.isNew) {
            Text(
                stringResource(R.string.stock_edit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownField(
            label = stringResource(R.string.field_unit),
            valueText = form.unit,
            options = ProductUnits.ALL.map { it to it },
            onSelect = { selectedUnit -> viewModel.edit { copy(unit = selectedUnit) } }
        )

        FormTextField(
            value = form.sku,
            onValueChange = { v -> viewModel.edit(FormField.SKU) { copy(sku = v) } },
            label = stringResource(R.string.field_sku),
            error = form.errors[FormField.SKU]
        )
        FormTextField(
            value = form.barcode,
            onValueChange = { v -> viewModel.edit(FormField.BARCODE) { copy(barcode = v) } },
            label = stringResource(R.string.field_barcode),
            error = form.errors[FormField.BARCODE]
        )
        FormTextField(
            value = form.description,
            onValueChange = { v -> viewModel.edit(FormField.DESCRIPTION) { copy(description = v) } },
            label = stringResource(R.string.field_description),
            error = form.errors[FormField.DESCRIPTION],
            singleLine = false
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = form.expiryDate?.format(DateFormat).orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.field_expiry)) },
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showDatePicker = true }) { Text(stringResource(R.string.expiry_set)) }
            if (form.expiryDate != null) {
                TextButton(onClick = { viewModel.edit { copy(expiryDate = null) } }) {
                    Text(stringResource(R.string.expiry_clear))
                }
            }
        }

        form.errors[FormField.GENERAL]?.let {
            Text(
                stringResource(it.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onClose,
                enabled = !form.saving,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.cancel)) }
            Button(
                onClick = { viewModel.save(onSaved = onClose) },
                enabled = !form.saving,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.save)) }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            viewModel.edit { copy(expiryDate = date) }
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: FormError?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        isError = error != null,
        supportingText = { if (error != null) Text(stringResource(error.messageRes())) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    valueText: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = valueText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
