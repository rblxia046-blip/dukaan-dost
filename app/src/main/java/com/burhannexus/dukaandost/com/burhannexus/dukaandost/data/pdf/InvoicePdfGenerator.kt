package com.burhannexus.dukaandost.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.burhannexus.dukaandost.core.InvoiceNumbers
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.domain.model.BusinessProfile
import com.burhannexus.dukaandost.domain.model.InvoiceDetail
import com.burhannexus.dukaandost.domain.model.InvoiceKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Words printed on the receipt. They are passed in so the PDF follows the app language. */
data class PdfLabels(
    val title: String,
    val partyLabel: String,
    val partyFallback: String,
    val phone: String,
    val item: String,
    val quantity: String,
    val price: String,
    val amount: String,
    val subtotal: String,
    val discount: String,
    val extraExpenses: String,
    val total: String,
    val returned: String,
    val paid: String,
    val refunded: String,
    val remaining: String,
    val method: String,
    val methodName: String
)

/**
 * Draws an A5 receipt: logo, shop name, address and phone at the top, the items, the totals,
 * and the shop's footer note. Long invoices continue on extra pages.
 */
@Singleton
class InvoicePdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

    suspend fun generate(
        invoice: InvoiceDetail,
        profile: BusinessProfile?,
        logoPng: ByteArray?,
        labels: PdfLabels
    ): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "receipts").apply { mkdirs() }
        val number = if (invoice.kind == InvoiceKind.SALE) InvoiceNumbers.sale(invoice.number)
        else InvoiceNumbers.purchase(invoice.number)
        val file = File(directory, "$number.pdf")

        val document = PdfDocument()
        try {
            ReceiptDrawer(document, invoice, profile, logoPng, labels, number, dateFormat).draw()
            file.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }
        file
    }
}

private const val PAGE_WIDTH = 420
private const val PAGE_HEIGHT = 595
private const val MARGIN = 28f
private const val RIGHT = PAGE_WIDTH - MARGIN
private const val QTY_RIGHT = 232f
private const val PRICE_RIGHT = 308f
private const val ITEM_WIDTH = QTY_RIGHT - MARGIN - 40f

private class ReceiptDrawer(
    private val document: PdfDocument,
    private val invoice: InvoiceDetail,
    private val profile: BusinessProfile?,
    private val logoPng: ByteArray?,
    private val labels: PdfLabels,
    private val number: String,
    private val dateFormat: DateTimeFormatter
) {
    private val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.BLACK }
    private val small = Paint(regular).apply { textSize = 8f; color = Color.DKGRAY }
    private val bold = Paint(regular).apply { typeface = Typeface.DEFAULT_BOLD }
    private val title = Paint(bold).apply { textSize = 12f }
    private val shop = Paint(bold).apply { textSize = 16f }
    private val line = Paint().apply { color = Color.GRAY; strokeWidth = 0.7f }

    private var pageNumber = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = MARGIN

    fun draw() {
        startPage()
        drawBusinessHeader()
        drawInvoiceHeading()
        drawTableHeader()
        invoice.lines.forEach { drawItem(it) }
        drawTotals()
        drawFooter()
        document.finishPage(page)
    }

    private fun startPage() {
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        y = MARGIN
    }

    private fun ensure(height: Float) {
        if (y + height <= PAGE_HEIGHT - MARGIN) return
        document.finishPage(page)
        startPage()
        canvas.drawText("$number (${pageNumber})", MARGIN, y + 9f, bold)
        y += 22f
        drawTableHeader()
    }

    private fun drawBusinessHeader() {
        val logo = logoPng?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.let { scaleToHeight(it, 50) }
        var textX = MARGIN
        if (logo != null) {
            canvas.drawBitmap(logo, MARGIN, y, null)
            textX = MARGIN + logo.width + 10f
        }
        val width = RIGHT - textX
        var textY = y + 14f
        val name = profile?.shopName?.ifBlank { null } ?: "Dukaan Dost"
        wrap(name, shop, width).take(2).forEach {
            canvas.drawText(it, textX, textY, shop)
            textY += 18f
        }
        profile?.address?.takeIf { it.isNotBlank() }?.let { address ->
            wrap(address, regular, width).take(3).forEach {
                canvas.drawText(it, textX, textY, regular)
                textY += 11f
            }
        }
        profile?.phone?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText("${labels.phone}: $it", textX, textY, regular)
            textY += 11f
        }
        val logoBottom = y + (logo?.height ?: 0)
        y = maxOf(textY, logoBottom.toFloat()) + 6f
        canvas.drawLine(MARGIN, y, RIGHT, y, line)
        y += 16f
    }

    private fun drawInvoiceHeading() {
        canvas.drawText("${labels.title} $number", MARGIN, y, title)
        val date = Instant.ofEpochMilli(invoice.createdAt).atZone(ZoneId.systemDefault()).format(dateFormat)
        canvas.drawText(date, RIGHT - regular.measureText(date), y, regular)
        y += 14f
        canvas.drawText(
            "${labels.partyLabel}: ${invoice.partyName ?: labels.partyFallback}",
            MARGIN, y, regular
        )
        y += 16f
    }

    private fun drawTableHeader() {
        canvas.drawText(labels.item, MARGIN, y, bold)
        right(labels.quantity, QTY_RIGHT, y, bold)
        right(labels.price, PRICE_RIGHT, y, bold)
        right(labels.amount, RIGHT, y, bold)
        y += 4f
        canvas.drawLine(MARGIN, y, RIGHT, y, line)
        y += 12f
    }

    private fun drawItem(item: com.burhannexus.dukaandost.domain.model.InvoiceLine) {
        ensure(if (item.discount.signum() > 0) 26f else 16f)
        canvas.drawText(fit(item.name, regular, ITEM_WIDTH + 40f), MARGIN, y, regular)
        right(Money.formatQuantity(item.quantity), QTY_RIGHT, y, regular)
        right(Money.format(item.unitPrice), PRICE_RIGHT, y, regular)
        right(Money.format(item.total), RIGHT, y, regular)
        y += 12f
        if (item.discount.signum() > 0) {
            canvas.drawText("  -" + Money.format(item.discount), MARGIN, y, small)
            y += 11f
        }
        y += 3f
    }

    private fun drawTotals() {
        ensure(130f)
        canvas.drawLine(MARGIN, y, RIGHT, y, line)
        y += 14f
        row(labels.subtotal, Money.format(invoice.subtotal), regular)
        if (invoice.invoiceDiscount.signum() > 0) row(labels.discount, "-" + Money.format(invoice.invoiceDiscount), regular)
        if (invoice.extraExpenses.signum() > 0) row(labels.extraExpenses, Money.format(invoice.extraExpenses), regular)
        row(labels.total, Money.format(invoice.total), title)
        if (invoice.returnedValue.signum() > 0) row(labels.returned, "-" + Money.format(invoice.returnedValue), regular)
        row(labels.paid, Money.format(invoice.paid), regular)
        if (invoice.refundedAmount.signum() > 0) row(labels.refunded, "-" + Money.format(invoice.refundedAmount), regular)
        if (invoice.remaining.signum() > 0) row(labels.remaining, Money.format(invoice.remaining), bold)
        row(labels.method, labels.methodName, small)
        y += 6f
    }

    private fun row(label: String, value: String, paint: Paint) {
        ensure(16f)
        canvas.drawText(label, 190f, y, paint)
        right(value, RIGHT, y, paint)
        y += if (paint === title) 16f else 13f
    }

    private fun drawFooter() {
        ensure(40f)
        y += 8f
        val note = profile?.footerNote.orEmpty()
        if (note.isNotBlank()) {
            wrap(note, regular, RIGHT - MARGIN).take(3).forEach {
                canvas.drawText(it, (PAGE_WIDTH - regular.measureText(it)) / 2f, y, regular)
                y += 12f
            }
        }
        val brand = "Generated by Dukaan Dost"
        canvas.drawText(brand, (PAGE_WIDTH - small.measureText(brand)) / 2f, y + 6f, small)
    }

    private fun right(text: String, rightX: Float, baseline: Float, paint: Paint) {
        canvas.drawText(text, rightX - paint.measureText(text), baseline, paint)
    }

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + paint.measureText(ellipsis) > maxWidth) end--
        return text.substring(0, end) + ellipsis
    }

    /** Splits text into lines that fit [maxWidth]. */
    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = ArrayList<String>()
        var rest = text.trim()
        while (rest.isNotEmpty()) {
            val count = paint.breakText(rest, true, maxWidth, null).coerceAtLeast(1)
            if (count >= rest.length) {
                lines.add(rest)
                break
            }
            val breakAt = rest.lastIndexOf(' ', count).let { if (it > 0) it else count }
            lines.add(rest.substring(0, breakAt).trim())
            rest = rest.substring(breakAt).trim()
        }
        return lines
    }

    private fun scaleToHeight(bitmap: Bitmap, height: Int): Bitmap {
        if (bitmap.height <= 0) return bitmap
        val width = (bitmap.width * (height.toFloat() / bitmap.height)).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width.coerceAtMost(120), height, true)
    }
}
