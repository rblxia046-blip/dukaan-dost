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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.domain.model.Category
import com.burhannexus.dukaandost.domain.model.CategoryResult
import com.burhannexus.dukaandost.domain.model.Product

@StringRes
private fun ProductSort.labelRes(): Int = when (this) {
    ProductSort.NAME -> R.string.sort_name
    ProductSort.LOWEST_STOCK -> R.string.sort_lowest_stock
    ProductSort.HIGHEST_STOCK -> R.string.sort_highest_stock
    ProductSort.SALE_PRICE_LOW -> R.string.sort_price_low
    ProductSort.SALE_PRICE_HIGH -> R.string.sort_price_high
}

@StringRes
internal fun CategoryResult.messageRes(): Int? = when (this) {
    CategoryResult.SUCCESS -> null
    CategoryResult.EMPTY_NAME -> R.string.category_err_empty
    CategoryResult.TOO_LONG -> R.string.category_err_long
    CategoryResult.DUPLICATE_NAME -> R.string.category_err_duplicate
    CategoryResult.NOT_FOUND -> R.string.category_err_not_found
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    contentPadding: PaddingValues,
    onAddProduct: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchText by rememberSaveable { mutableStateOf("") }
    var showCategories by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.onQueryChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.products_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = ""
                                viewModel.onQueryChange("")
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    }
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedCategoryId == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text(stringResource(R.string.filter_all)) }
                        )
                    }
                    items(state.categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = state.selectedCategoryId == category.id,
                            onClick = {
                                viewModel.selectCategory(
                                    if (state.selectedCategoryId == category.id) null else category.id
                                )
                            },
                            label = { Text(category.name) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showCategories = true },
                            label = { Text(stringResource(R.string.manage_categories)) }
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.products_count, state.products.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        TextButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.sort_by, stringResource(state.sort.labelRes())))
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            ProductSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.labelRes())) },
                                    leadingIcon = {
                                        if (option == state.sort) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectSort(option)
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SummaryRow(stringResource(R.string.stock_value_shown), Money.format(state.stockValue))
                        SummaryRow(stringResource(R.string.expected_profit_shown), Money.format(state.expectedProfit))
                    }
                }
            }

            if (state.products.isEmpty() && !state.isLoading) {
                item {
                    val filtering = searchText.isNotBlank() || state.selectedCategoryId != null
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(
                                if (filtering) R.string.products_no_match else R.string.products_empty_title
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        if (!filtering) {
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.products_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(state.products, key = { it.id }) { product ->
                    ProductCard(product, onClick = { onOpenProduct(product.id) })
                }
            }
        }

        FloatingActionButton(
            onClick = onAddProduct,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_product))
        }
    }

    if (showCategories) {
        ManageCategoriesDialog(
            categories = state.categories,
            onDismiss = { showCategories = false },
            onAdd = viewModel::addCategory,
            onRename = viewModel::renameCategory,
            onDelete = viewModel::deleteCategory
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                val subtitle = listOfNotNull(product.categoryName, product.sku).joinToString(" | ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    stringResource(R.string.sale_price_short, Money.format(product.salePrice)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${Money.formatQuantity(product.stock)} ${product.unit}",
                    style = MaterialTheme.typography.titleMedium
                )
                when {
                    product.isOutOfStock -> StatusBadge(R.string.badge_out_of_stock, error = true)
                    product.isLowStock -> StatusBadge(R.string.badge_low_stock, error = false)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(@StringRes textRes: Int, error: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun ManageCategoriesDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (String, (CategoryResult) -> Unit) -> Unit,
    onRename: (Long, String, (CategoryResult) -> Unit) -> Unit,
    onDelete: (Long) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<Int?>(null) }
    var renaming by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.categories_title)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            addError = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = addError != null,
                        placeholder = { Text(stringResource(R.string.category_new_hint)) }
                    )
                    Button(onClick = {
                        onAdd(newName) { result ->
                            if (result == CategoryResult.SUCCESS) {
                                newName = ""
                                addError = null
                            } else {
                                addError = result.messageRes()
                            }
                        }
                    }) { Text(stringResource(R.string.add)) }
                }
                addError?.let {
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                if (categories.isEmpty()) {
                    Text(
                        stringResource(R.string.category_none_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(categories, key = { it.id }) { category ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(category.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        stringResource(R.string.category_products_count, category.productCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { renaming = category }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename))
                                }
                                IconButton(onClick = { deleting = category }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )

    renaming?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.name) }
        var error by remember(target.id) { mutableStateOf<Int?>(null) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.category_rename_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            error = null
                        },
                        singleLine = true,
                        isError = error != null
                    )
                    error?.let {
                        Text(
                            stringResource(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(target.id, text) { result ->
                        if (result == CategoryResult.SUCCESS) renaming = null else error = result.messageRes()
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.category_delete_title)) },
            text = { Text(stringResource(R.string.category_delete_body, target.name, target.productCount)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleting = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
