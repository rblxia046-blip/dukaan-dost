package com.burhannexus.dukaandost.ui.business

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.ui.common.DeveloperFooter
import com.burhannexus.dukaandost.ui.trade.asString

@StringRes
private fun BizError.messageRes(): Int = when (this) {
    BizError.REQUIRED -> R.string.err_required
    BizError.INVALID_PHONE -> R.string.biz_err_phone
    BizError.TOO_LONG -> R.string.err_too_long
}

/**
 * Shop details used on every receipt. Shown full-screen the first time the app opens
 * ([firstRun]) and from Settings afterwards.
 */
@Composable
fun BusinessProfileScreen(
    contentPadding: PaddingValues,
    firstRun: Boolean,
    onSaved: () -> Unit,
    viewModel: BusinessProfileViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val logo by viewModel.logo.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setLogo(uri.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (firstRun) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(stringResource(R.string.biz_welcome_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.biz_welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(stringResource(R.string.biz_title), style = MaterialTheme.typography.titleLarge)
        }

        BusinessTextField(
            value = form.shopName,
            onChange = { v -> viewModel.edit(BizField.SHOP_NAME) { copy(shopName = v) } },
            label = stringResource(R.string.biz_shop_name),
            error = form.errors[BizField.SHOP_NAME]
        )
        BusinessTextField(
            value = form.phone,
            onChange = { v -> viewModel.edit(BizField.PHONE) { copy(phone = v) } },
            label = stringResource(R.string.biz_phone),
            error = form.errors[BizField.PHONE],
            keyboardType = KeyboardType.Phone
        )
        BusinessTextField(
            value = form.address,
            onChange = { v -> viewModel.edit(BizField.ADDRESS) { copy(address = v) } },
            label = stringResource(R.string.biz_address),
            error = form.errors[BizField.ADDRESS],
            singleLine = false
        )
        BusinessTextField(
            value = form.ownerName,
            onChange = { v -> viewModel.edit(BizField.OWNER_NAME) { copy(ownerName = v) } },
            label = stringResource(R.string.biz_owner),
            error = form.errors[BizField.OWNER_NAME]
        )
        BusinessTextField(
            value = form.footerNote,
            onChange = { v -> viewModel.edit(BizField.FOOTER_NOTE) { copy(footerNote = v) } },
            label = stringResource(R.string.biz_footer_note),
            error = form.errors[BizField.FOOTER_NOTE],
            singleLine = false
        )

        Text(stringResource(R.string.biz_logo), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                LogoPreview(logo)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(if (logo == null) R.string.biz_logo_choose else R.string.biz_logo_change)) }
                if (logo != null) {
                    TextButton(onClick = viewModel::removeLogo) { Text(stringResource(R.string.biz_logo_remove)) }
                }
            }
        }
        Text(
            stringResource(R.string.biz_logo_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        message?.let {
            Text(it.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { viewModel.save(onSaved) },
            enabled = !form.saving,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(if (firstRun) R.string.biz_start else R.string.save)) }

        DeveloperFooter()
    }
}

@Composable
fun LogoPreview(png: ByteArray?, modifier: Modifier = Modifier) {
    val bitmap = remember(png) {
        png?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(R.string.biz_logo),
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    } else {
        Text(
            stringResource(R.string.biz_logo_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BusinessTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: BizError?,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        isError = error != null,
        supportingText = { if (error != null) Text(stringResource(error.messageRes())) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}
