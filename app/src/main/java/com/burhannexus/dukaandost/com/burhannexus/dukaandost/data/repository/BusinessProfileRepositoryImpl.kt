package com.burhannexus.dukaandost.data.repository

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.burhannexus.dukaandost.data.local.LogoStore
import com.burhannexus.dukaandost.domain.model.BusinessProfile
import com.burhannexus.dukaandost.domain.model.BusinessProfileInput
import com.burhannexus.dukaandost.domain.repository.BusinessProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logoStore: LogoStore
) : BusinessProfileRepository {

    override val profile: Flow<BusinessProfile?> = dataStore.data.map { prefs ->
        if (prefs[SETUP_DONE] != true) {
            null
        } else {
            BusinessProfile(
                shopName = prefs[SHOP_NAME].orEmpty(),
                ownerName = prefs[OWNER_NAME].orEmpty(),
                phone = prefs[PHONE].orEmpty(),
                address = prefs[ADDRESS].orEmpty(),
                footerNote = prefs[FOOTER_NOTE].orEmpty(),
                logoVersion = prefs[LOGO_VERSION] ?: 0L
            )
        }
    }.distinctUntilChanged()

    override val logoVersion: Flow<Long> = dataStore.data
        .map { it[LOGO_VERSION] ?: 0L }
        .distinctUntilChanged()

    override suspend fun save(input: BusinessProfileInput) {
        dataStore.edit { prefs ->
            prefs[SHOP_NAME] = input.shopName.trim()
            prefs[OWNER_NAME] = input.ownerName.trim()
            prefs[PHONE] = input.phone.trim()
            prefs[ADDRESS] = input.address.trim()
            prefs[FOOTER_NOTE] = input.footerNote.trim()
            prefs[SETUP_DONE] = true
        }
    }

    override suspend fun setLogo(uri: String): Boolean {
        val saved = logoStore.saveFromUri(Uri.parse(uri))
        if (saved) dataStore.edit { it[LOGO_VERSION] = System.currentTimeMillis() }
        return saved
    }

    override suspend fun removeLogo() {
        logoStore.delete()
        dataStore.edit { it[LOGO_VERSION] = 0L }
    }

    override suspend fun loadLogoPng(): ByteArray? = logoStore.readBytes()

    private companion object {
        val SETUP_DONE = booleanPreferencesKey("business_setup_done")
        val SHOP_NAME = stringPreferencesKey("business_shop_name")
        val OWNER_NAME = stringPreferencesKey("business_owner_name")
        val PHONE = stringPreferencesKey("business_phone")
        val ADDRESS = stringPreferencesKey("business_address")
        val FOOTER_NOTE = stringPreferencesKey("business_footer_note")
        val LOGO_VERSION = longPreferencesKey("business_logo_version")
    }
}
