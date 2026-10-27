package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

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
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNull_or_blank()) {
                            title = name.removeSuffix(".pdf").replace("_", " ").replace("-", " ")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    pageCount = renderer.pageCount.coerceAtLeast(1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return BookMetadata(title = title, author = "PDF Document", pageCount = pageCount)
    }

    private fun String.isNull_or_blank(): Boolean = this.isBlank()

    fun renderPageBitmap(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val safePageIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(safePageIndex).use { page ->
                        val aspectRatio = page.height.toFloat() / page.width.toFloat()
                        val width = targetWidth
                        val height = (width * aspectRatio).toInt().coerceAtLeast(100)

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun extractPageText(context: Context, uri: Uri, pageIndex: Int): String {
        // Stream text extraction logic for PDF text streams
        val extractedText = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    var lineCount = 0
                    while (reader.readLine().also { line = it } != null && lineCount < 2000) {
                        val currentLine = line ?: ""
                        if (currentLine.contains("BT") || currentLine.contains("Tj") || currentLine.contains("TJ")) {
                            // Extract text literals inside parenthesis (...) or brackets [...]
                            val textMatches = Regex("\\(([^)]+)\\)").findAll(currentLine)
                            for (match in textMatches) {
                                val matchText = match.groupValues[1]
                                if (matchText.length > 2 && !matchText.startsWith("/")) {
                                    extractedText.append(matchText).append(" ")
                                }
                            }
                        }
                        lineCount++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val text = extractedText.toString().trim()
        if (text.length > 30) {
            return text
        }

        // Fallback placeholder text explaining page content for TTS or manual review
        return "Page ${pageIndex + 1}. Select Read Aloud to listen to available document text, or tap next paragraph to navigate sequentially."
    }
}
