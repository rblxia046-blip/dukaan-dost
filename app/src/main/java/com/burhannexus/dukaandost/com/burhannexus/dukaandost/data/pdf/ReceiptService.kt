package com.burhannexus.dukaandost.data.pdf

import com.burhannexus.dukaandost.domain.model.InvoiceDetail
import com.burhannexus.dukaandost.domain.repository.BusinessProfileRepository
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Builds the receipt PDF with the shop's current profile and logo. */
@Singleton
class ReceiptService @Inject constructor(
    private val profiles: BusinessProfileRepository,
    private val generator: InvoicePdfGenerator,
    private val statements: StatementPdfGenerator
) {
    suspend fun createPdf(invoice: InvoiceDetail, labels: PdfLabels): File {
        val profile = profiles.profile.first()
        val logo = if (profile?.hasLogo == true) profiles.loadLogoPng() else null
        return generator.generate(invoice, profile, logo, labels)
    }

    suspend fun createStatementPdf(rows: List<StatementRow>, labels: StatementLabels): File {
        val profile = profiles.profile.first()
        val logo = if (profile?.hasLogo == true) profiles.loadLogoPng() else null
        return statements.generate(rows, profile, logo, labels)
    }
}
