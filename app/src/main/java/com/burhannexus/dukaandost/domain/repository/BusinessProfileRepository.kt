package com.burhannexus.dukaandost.domain.repository

import com.burhannexus.dukaandost.domain.model.BusinessProfile
import com.burhannexus.dukaandost.domain.model.BusinessProfileInput
import kotlinx.coroutines.flow.Flow

interface BusinessProfileRepository {
    /** null until the first-run setup has been completed. */
    val profile: Flow<BusinessProfile?>

    /** Changes every time the logo is set or removed (0 = no logo). */
    val logoVersion: Flow<Long>

    suspend fun save(input: BusinessProfileInput)

    /** Copies the chosen image into app storage (scaled down). Returns false when it cannot be read. */
    suspend fun setLogo(uri: String): Boolean
    suspend fun removeLogo()

    /** PNG bytes of the logo, or null when there is none. */
    suspend fun loadLogoPng(): ByteArray?
}
