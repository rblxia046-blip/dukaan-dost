package com.burhannexus.dukaandost.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.domain.model.DashboardPeriod
import com.burhannexus.dukaandost.ui.common.DeveloperFooter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private data class Metric(
    @StringRes val labelRes: Int,
    val value: String,
    val icon: ImageVector
)

@StringRes
private fun DashboardPeriod.labelRes(): Int = when (this) {
    DashboardPeriod.TODAY -> R.string.period_today
    DashboardPeriod.YESTERDAY -> R.string.period_yesterday
    DashboardPeriod.THIS_WEEK -> R.string.period_this_week
    DashboardPeriod.THIS_MONTH -> R.string.period_this_month
    DashboardPeriod.THIS_YEAR -> R.string.period_this_year
    DashboardPeriod.CUSTOM -> R.string.period_custom
}

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomDialog by remember { mutableStateOf(false) }
    val s = state.summary

    val metrics = listOf(
        Metric(R.string.metric_sales, Money.format(s.sales), Icons.Filled.PointOfSale),
        Metric(R.string.metric_profit, Money.format(s.profit), Icons.Filled.TrendingUp),
        Metric(R.string.metric_total_products, s.totalProducts.toString(), Icons.Filled.Inventory2),
        Metric(R.string.metric_total_stock, Money.formatQuantity(s.totalStockQuantity), Icons.Filled.Warehouse),
        Metric(R.string.metric_low_stock, s.lowStockProducts.toString(), Icons.Filled.ProductionQuantityLimits),
        Metric(R.string.metric_out_of_stock, s.outOfStockProducts.toString(), Icons.Filled.RemoveShoppingCart),
        Metric(R.string.metric_receivables, Money.format(s.customerReceivables), Icons.Filled.People),
        Metric(R.string.metric_payables, Money.format(s.supplierPayables), Icons.Filled.LocalShipping),
        Metric(R.string.metric_expenses, Money.format(s.expenses), Icons.Filled.Payments),
        Metric(R.string.metric_cash_balance, Money.format(s.cashBalance), Icons.Filled.AccountBalanceWallet)
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DashboardPeriod.entries.toList()) { period ->
                    FilterChip(
                        selected = state.filter.period == period,
                        onClick = {
                            if (period == DashboardPeriod.CUSTOM) showCustomDialog = true
                            else viewModel.selectPeriod(period)
                        },
                        label = { Text(stringResource(period.labelRes())) }
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(
                    R.string.showing_range,
                    state.range.start.format(DateFormat),
                    state.range.end.format(DateFormat)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(metrics.chunked(2)) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { MetricCard(it, Modifier.weight(1f)) }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item { EmptyInfoCard(R.string.chart_monthly_sales, R.string.chart_empty) }
        item { EmptyInfoCard(R.string.chart_monthly_profit, R.string.chart_empty) }
        item { EmptyInfoCard(R.string.recent_sales, R.string.recent_empty) }
        item { EmptyInfoCard(R.string.recent_purchases, R.string.recent_empty) }
        item {
            Text(
                text = stringResource(R.string.dashboard_data_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { DeveloperFooter() }
    }

    if (showCustomDialog) {
        CustomRangeDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { start, end ->
                showCustomDialog = false
                viewModel.selectCustomRange(start, end)
            }
        )
    }
}

@Composable
private fun MetricCard(metric: Metric, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
            Text(
                text = stringResource(metric.labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyInfoCard(@StringRes titleRes: Int, @StringRes messageRes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val pickerState = rememberDateRangePickerState()
    val startMillis = pickerState.selectedStartDateMillis
    val endMillis = pickerState.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = startMillis != null && endMillis != null,
                onClick = {
                    if (startMillis != null && endMillis != null) {
                        onConfirm(startMillis.toLocalDateUtc(), endMillis.toLocalDateUtc())
                    }
                }
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    ) {
        DateRangePicker(state = pickerState, modifier = Modifier.height(500.dp))
    }
}

/** Material date pickers report UTC midnight; convert without timezone drift. */
private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
