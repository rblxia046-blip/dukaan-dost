package com.burhannexus.dukaandost.ui.parties

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.domain.model.PartyKind

/** The Ledger menu: pick Customers or Suppliers, then a party, to see its account. */
@Composable
fun LedgerHomeScreen(
    contentPadding: PaddingValues,
    onOpenLedger: (PartyKind, Long) -> Unit,
    viewModel: PartiesViewModel = hiltViewModel()
) {
    var kindName by rememberSaveable { mutableStateOf(PartyKind.CUSTOMER.name) }
    val kind = PartyKind.valueOf(kindName)
    LaunchedEffect(kind) {
        viewModel.setQuery("")
        viewModel.start(kind)
    }
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(stringResource(R.string.ledger_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.ledger_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = kind == PartyKind.CUSTOMER,
                    onClick = { kindName = PartyKind.CUSTOMER.name },
                    label = { Text(stringResource(R.string.nav_customers)) }
                )
                FilterChip(
                    selected = kind == PartyKind.SUPPLIER,
                    onClick = { kindName = PartyKind.SUPPLIER.name },
                    label = { Text(stringResource(R.string.nav_suppliers)) }
                )
            }
        }
        item { OutstandingSummary(kind, accounts) }
        if (accounts.isEmpty()) {
            item {
                Text(
                    stringResource(if (kind == PartyKind.CUSTOMER) R.string.customers_empty else R.string.suppliers_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(accounts, key = { it.id }) { account ->
                PartyAccountCard(account = account, kind = kind, onClick = { onOpenLedger(kind, account.id) })
            }
        }
    }
}
