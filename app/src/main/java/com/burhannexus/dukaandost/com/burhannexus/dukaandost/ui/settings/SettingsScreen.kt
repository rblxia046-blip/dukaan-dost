package com.burhannexus.dukaandost.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.ui.business.LogoPreview
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.burhannexus.dukaandost.BuildConfig
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.DeveloperInfo
import com.burhannexus.dukaandost.ui.common.DeveloperFooter

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onEditBusiness: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val logo by viewModel.logo.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.biz_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (logo != null) {
                        Box(Modifier.size(64.dp)) { LogoPreview(logo) }
                    }
                    Column(Modifier.weight(1f)) {
                        profile?.let { p ->
                            Text(p.shopName, style = MaterialTheme.typography.titleLarge)
                            Text(p.phone, style = MaterialTheme.typography.bodyMedium)
                            if (p.address.isNotEmpty()) Text(p.address, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onEditBusiness) { Text(stringResource(R.string.biz_edit)) }
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "${stringResource(R.string.app_version)}: ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.developer_information),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(stringResource(R.string.developed_by, DeveloperInfo.BRAND), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.phone_label, DeveloperInfo.PHONE), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.email_label, DeveloperInfo.EMAIL), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        DeveloperFooter()
    }
}
