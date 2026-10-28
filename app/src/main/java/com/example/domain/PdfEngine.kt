package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

data class BookMetadata(
    val title: String,
    val author: String = "Unknown Author",
    val pageCount: Int = 1
)

object PdfEngine {
    fun extractBookMetadata(context: Context, uri: Uri): BookMetadata {
        var title = "Imported PDF Document"
        var pageCount = 1
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)?.takeIf { it.isNotBlank() }?.let {
                            title = it.removeSuffix(".pdf").replace("_", " ").replace("-", " ")
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { pageCount = it.pageCount.coerceAtLeast(1) }
            }
        } catch (_: Exception) { }
        return BookMetadata(title, "PDF Document", pageCount)
    }

    fun renderPageBitmap(context: Context, uri: Uri, pageIndex: Int, targetWidth: Int = 1080): Bitmap? =
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return null
                    renderer.openPage(pageIndex.coerceIn(0, renderer.pageCount - 1)).use { page ->
                        val width = targetWidth
                        val height = (width * page.height.toFloat() / page.width.toFloat()).toInt().coerceAtLeast(100)
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }
        } catch (_: Exception) { null }

    /**
     * Extract real page text with PDFBox. PDF-rendering APIs alone cannot extract text.
     * Scanned/image-only PDFs need OCR and return an empty string instead of fake placeholder text.
     */
    fun extractPageText(context: Context, uri: Uri, pageIndex: Int): String {
        return try {
            PDFBoxResourceLoader.init(context.applicationContext)
            context.contentResolver.openInputStream(uri)?.use { input ->
                PDDocument.load(input).use { document ->
                    if (document.numberOfPages == 0) return ""
                    val page = (pageIndex + 1).coerceIn(1, document.numberOfPages)
                    PDFTextStripper().apply {
                        startPage = page
                        endPage = page
                    }.getText(document).replace("\u0000", "").trim()
                }
            }.orEmpty()
        } catch (_: Exception) { "" }
    }
}
