package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BookAiService
import com.example.domain.PdfEngine
import com.example.domain.TtsNarrationEngine
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.WorkoutOrange
import com.example.ui.viewmodel.HealthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    viewModel: HealthViewModel,
    bookId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allBooks by viewModel.allBooks.collectAsState()
    val book = remember(allBooks, bookId) { allBooks.find { it.id == bookId } }

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HealthTeal)
        }
        return
    }

    var currentPage by remember { mutableIntStateOf(book.lastPageRead.coerceIn(0, (book.pageCount - 1).coerceAtLeast(0))) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageText by remember { mutableStateOf("") }
    var paragraphs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isTextMode by remember { mutableStateOf(false) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var continueNarrationAcrossPages by remember { mutableStateOf(false) }

    // AI Dialog state
    var showAiSummaryDialog by remember { mutableStateOf(false) }
    var aiSummaryText by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }\n    var selectedLanguage by remember(book.languageCode) { mutableStateOf(book.languageCode.ifBlank { "ne" }) }\n    var showLanguageMenu by remember { mutableStateOf(false) }\n    var isTranslating by remember { mutableStateOf(false) }\n    var translatedText by remember { mutableStateOf("") }\n    var translationError by remember { mutableStateOf<String?>(null) }

    // TTS Narration Engine
    val ttsEngine = remember {
        TtsNarrationEngine(
            context = context,
            onParagraphChanged = { _ -> },
            onPageFinished = {
                if (currentPage < book.pageCount - 1) {
                    continueNarrationAcrossPages = true
                    currentPage += 1
                }
            }
        )
    }

    val isPlaying by ttsEngine.isPlaying.collectAsState()
    val currentParagraphIndex by ttsEngine.currentParagraphIndex.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            ttsEngine.shutdown()
        }
    }

    // Render page bitmap and extract text whenever currentPage changes
    LaunchedEffect(currentPage, book.uriString) {
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(book.uriString)
            val bmp = PdfEngine.renderPageBitmap(context, uri, currentPage)
            val text = PdfEngine.extractPageText(context, uri, currentPage)
            val paraList = text.split("\n\n", "\n").filter { it.isNotBlank() }

            withContext(Dispatchers.Main) {
                pageBitmap = bmp
                pageText = text
                paragraphs = paraList
                viewModel.updateBookProgress(bookId, currentPage, book.pageCount)
                if (continueNarrationAcrossPages && paraList.isNotEmpty()) {
                    continueNarrationAcrossPages = false
                    translatedText = ""
                    coroutineScope.launch {
                        isTranslating = true
                        try {
                            translatedText = BookAiService.translate(text, languageName(selectedLanguage))
                            ttsEngine.startReadingPage(
                                translatedText.split("\\n\\n", "\\n").filter { it.isNotBlank() },
                                0,
                                speechRate,
                                selectedLanguage
                            )
                        } catch (e: Exception) {
                            translationError = e.message ?: "Translation failed. Please retry."
                            ttsEngine.startReadingPage(paraList, 0, speechRate, "en")
                        } finally {
                            isTranslating = false
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 16.sp)
                        Text("Page ${currentPage + 1} of ${book.pageCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        ttsEngine.stop()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { showLanguageMenu = true }) {
                            Text("Translate: ${languageName(selectedLanguage)}", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = showLanguageMenu, onDismissRequest = { showLanguageMenu = false }) {
                            listOf("ne" to "नेपाली", "hi" to "हिन्दी", "en" to "English", "bn" to "বাংলা", "ur" to "اردو", "es" to "Español", "fr" to "Français", "pt" to "Português", "ar" to "العربية", "zh" to "中文", "ja" to "日本語", "ko" to "한국어").forEach { (code, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    selectedLanguage = code
                                    translatedText = ""
                                    translationError = null
                                    showLanguageMenu = false
                                })
                            }
                        }
                    }
                    IconButton(onClick = {
                        if (pageText.isBlank() || pageText.startsWith("Page ${currentPage + 1}.")) {
                            translationError = "No real PDF text was extracted. This may be a scanned PDF; OCR is needed."
                        } else {
                            isTranslating = true
                            translationError = null
                            coroutineScope.launch {
                                try {
                                    translatedText = BookAiService.translate(pageText, languageName(selectedLanguage))
                                    isTextMode = true
                                } catch (e: Exception) {
                                    translationError = e.message ?: "Translation failed. Please retry."
                                } finally { isTranslating = false }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Translate, contentDescription = "Translate book", tint = HealthTeal)
                    }
                    IconButton(onClick = { isTextMode = !isTextMode }) {
                        Icon(if (isTextMode) Icons.Default.Image else Icons.Default.TextFields, contentDescription = "Toggle View")
                    }
                    IconButton(onClick = {
                        viewModel.addBookmark(bookId, currentPage, "Bookmark Page ${currentPage + 1}")
                        Toast.makeText(context, "Page ${currentPage + 1} Bookmarked!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = HealthTeal)
                    }
                    IconButton(onClick = {
                        showAiSummaryDialog = true
                        isAiLoading = true
                        coroutineScope.launch {
                            try {
                                aiSummaryText = BookAiService.summarize(pageText, languageName(selectedLanguage))
                            } catch (e: Exception) {
                                aiSummaryText = e.message ?: "AI summary failed. Please retry."
                            } finally {
                                isAiLoading = false
                            }
                        }
                    }) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI Summary", tint = HealthTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Audio Narration & Walk Player Bar
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isTranslating) {\n                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = HealthTeal)\n                        Text("Translating into ${languageName(selectedLanguage)}…", fontSize = 12.sp)\n                    }\n                    translationError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }\n                    // Page slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("${currentPage + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { currentPage = it.toInt() },
                            valueRange = 0f..(book.pageCount - 1).coerceAtLeast(1).toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = HealthTeal, activeTrackColor = HealthTeal)
                        )
                        Text("${book.pageCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Audio Playback Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            speechRate = when (speechRate) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                2.0f -> 0.75f
                                else -> 1.0f
                            }
                            ttsEngine.setSpeed(speechRate)
                        }) {
                            Text("${speechRate}x", fontWeight = FontWeight.Bold, color = HealthTeal)
                        }

                        IconButton(onClick = { ttsEngine.previousParagraph() }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Paragraph")
                        }

                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    ttsEngine.pause()
                                } else {
                                    coroutineScope.launch {
                                        if (translatedText.isBlank()) {
                                            isTranslating = true
                                            translationError = null
                                            try {
                                                translatedText = BookAiService.translate(pageText, languageName(selectedLanguage))
                                            } catch (e: Exception) {
                                                translationError = e.message ?: "Translation failed. Please retry."
                                            } finally {
                                                isTranslating = false
                                            }
                                        }
                                        val speechText = if (translatedText.isNotBlank()) translatedText else pageText
                                        val speechParagraphs = speechText.split("\\n\\n", "\\n").filter { it.isNotBlank() }
                                        ttsEngine.startReadingPage(speechParagraphs, currentParagraphIndex, speechRate, if (translatedText.isNotBlank()) selectedLanguage else "en")
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(HealthTeal)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Narration" else "Read Aloud",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(onClick = { ttsEngine.nextParagraph() }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Paragraph")
                        }

                        IconButton(onClick = {
                            if (currentPage < book.pageCount - 1) currentPage += 1
                        }) {
                            Icon(Icons.Default.NavigateNext, contentDescription = "Next Page")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            if (isTextMode) {
                // Text View Mode with Paragraph Highlighting
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(if (translatedText.isNotBlank()) translatedText.split("\\n\\n", "\\n").filter { it.isNotBlank() } else paragraphs) { index, para ->
                        val isCurrent = index == currentParagraphIndex && isPlaying
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) HealthTeal.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, HealthTeal) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = para,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(14.dp),
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            } else {
                // Image Bitmap Mode
                if (pageBitmap != null) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page ${currentPage + 1}",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(color = HealthTeal)
                }
            }
        }
    }

    // AI Summary Dialog
    if (showAiSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showAiSummaryDialog = false },
            title = { Text("AI Page Study Notes & Summary") },
            text = {
                if (isAiLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = HealthTeal, modifier = Modifier.size(24.dp))
                        Text("Analyzing page content with Gemini AI...")
                    }
                } else {
                    Text(aiSummaryText, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(onClick = { showAiSummaryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}


private fun languageName(code: String): String = when (code) {
    "ne" -> "Nepali"
    "hi" -> "Hindi"
    "en" -> "English"
    "bn" -> "Bengali"
    "ur" -> "Urdu"
    "es" -> "Spanish"
    "fr" -> "French"
    "pt" -> "Portuguese"
    "ar" -> "Arabic"
    "zh" -> "Chinese"
    "ja" -> "Japanese"
    "ko" -> "Korean"
    else -> "Nepali"
}
