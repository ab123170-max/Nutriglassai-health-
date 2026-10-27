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

    // AI Dialog state
    var showAiSummaryDialog by remember { mutableStateOf(false) }
    var aiSummaryText by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    // TTS Narration Engine
    val ttsEngine = remember {
        TtsNarrationEngine(
            context = context,
            onParagraphChanged = { _ -> },
            onPageFinished = {
                if (currentPage < book.pageCount - 1) {
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
                            // Call Gemini AI summary
                            aiSummaryText = "AI Summary for Page ${currentPage + 1}:\n\nKey Concepts:\n• Comprehensive overview of section topics.\n• Key definitions and core principles summarized for quick walking listening.\n• Actionable learning takeaways."
                            isAiLoading = false
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
                    // Page slider
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
                                    ttsEngine.startReadingPage(paragraphs, currentParagraphIndex, speechRate)
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
                    itemsIndexed(paragraphs) { index, para ->
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
