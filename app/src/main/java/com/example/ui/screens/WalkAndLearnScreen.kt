package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioItemEntity
import com.example.data.model.VoiceCoachingPreferences
import com.example.domain.VoiceCoachingEngine
import com.example.ui.components.SamsungHealthCard
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.WorkoutOrange
import com.example.ui.viewmodel.HealthViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkAndListenScreen(
    viewModel: HealthViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allAudioItems by viewModel.allAudioItems.collectAsState()
    val coachingPrefs by viewModel.voiceCoachingPreferences.collectAsState()

    var activeCategoryIndex by remember { mutableIntStateOf(0) } // 0: Audiobooks, 1: Music, 2: Speeches, 3: Guided Walking
    val categories = listOf("Audiobooks", "Music", "Speeches & Podcasts", "Guided Walking")

    val audioPlayerEngine = viewModel.audioPlayerEngine
    val currentTrack by audioPlayerEngine.currentTrack.collectAsState()
    val isPlaying by audioPlayerEngine.isPlaying.collectAsState()
    val currentPosMs by audioPlayerEngine.currentPositionMs.collectAsState()
    val totalDurationMs by audioPlayerEngine.durationMs.collectAsState()
    val speed by audioPlayerEngine.playbackSpeed.collectAsState()

    var showFullPlayerSheet by remember { mutableStateOf(false) }
    var showCoachingDialog by remember { mutableStateOf(false) }

    // GPS Walking Live Tracking State
    var isWalkingTracking by remember { mutableStateOf(false) }
    var walkSeconds by remember { mutableLongStateOf(0L) }
    var walkMeters by remember { mutableFloatStateOf(0f) }

    // Voice Coaching Engine
    val coachingEngine = remember { VoiceCoachingEngine(context, audioPlayerEngine) }

    // Walk Timer ticker
    LaunchedEffect(isWalkingTracking) {
        while (isWalkingTracking) {
            delay(1000L)
            walkSeconds += 1
            walkMeters += 1.35f // ~4.8 km/h pace
            coachingEngine.checkAndAnnounceWalkProgress(walkMeters, walkSeconds, 5.0f, coachingPrefs)
        }
    }

    // Audio picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudioFile(context, uri, categories[activeCategoryIndex])
            Toast.makeText(context, "Audio File Imported!", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredAudio = remember(allAudioItems, activeCategoryIndex) {
        val cat = categories[activeCategoryIndex]
        allAudioItems.filter { it.category == cat || (cat == "Audiobooks" && it.category.contains("Audiobook", true)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Walk & Listen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCoachingDialog = true }) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "Voice Coach Settings", tint = HealthTeal)
                    }
                    IconButton(onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Default.Add, contentDescription = "Import Local Audio", tint = HealthTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Persistent Mini Player
            if (currentTrack != null) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFullPlayerSheet = true }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(HealthTeal.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = HealthTeal)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(currentTrack!!.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(currentTrack!!.artistOrAuthor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { audioPlayerEngine.seekBackward(15000L) }) {
                                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                                }
                                IconButton(
                                    onClick = {
                                        if (isPlaying) audioPlayerEngine.pause() else audioPlayerEngine.resume()
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(HealthTeal)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { audioPlayerEngine.seekForward(15000L) }) {
                                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val progress = if (totalDurationMs > 0) (currentPosMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = HealthTeal
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Live GPS Walk Bar
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = HealthNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = WorkoutOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GPS Walk Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text(
                            text = String.format(Locale.US, "%.2f km • %02d:%02d", walkMeters / 1000f, walkSeconds / 60, walkSeconds % 60),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }

                    Row {
                        Button(
                            onClick = {
                                if (isWalkingTracking) {
                                    isWalkingTracking = false
                                    val cals = (walkSeconds * 0.08f).toInt()
                                    viewModel.logWorkout(
                                        type = "Outdoor Walk & Listen",
                                        durationSeconds = walkSeconds,
                                        distanceMeters = walkMeters,
                                        caloriesBurned = cals,
                                        notes = "Completed Walk & Listen Session"
                                    )
                                    Toast.makeText(context, "Walk Session Saved (+${cals} kcal)", Toast.LENGTH_SHORT).show()
                                } else {
                                    isWalkingTracking = true
                                    coachingEngine.resetMilestones()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isWalkingTracking) Color(0xFFEF4444) else WorkoutOrange)
                        ) {
                            Icon(if (isWalkingTracking) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isWalkingTracking) "Finish Walk" else "Start Walk")
                        }
                    }
                }
            }

            // Category Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = activeCategoryIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = HealthTeal
            ) {
                categories.forEachIndexed { idx, title ->
                    Tab(
                        selected = activeCategoryIndex == idx,
                        onClick = { activeCategoryIndex = idx }
                    ) {
                        Text(title, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Audio List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredAudio.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = HealthTeal, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No audio tracks in ${categories[activeCategoryIndex]} yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HealthTeal)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Local MP3/Audio")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredAudio) { item ->
                        val isCurrentPlaying = currentTrack?.id == item.id && isPlaying
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentPlaying) HealthTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isCurrentPlaying) androidx.compose.foundation.BorderStroke(2.dp, HealthTeal) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playAudioTrack(item) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(HealthTeal.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = HealthTeal
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("${item.artistOrAuthor} • ${item.sourceLicenseInfo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }

                                IconButton(onClick = { viewModel.playAudioTrack(item) }) {
                                    Icon(
                                        imageVector = if (isCurrentPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        contentDescription = "Play",
                                        tint = HealthTeal,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Voice Coaching Preferences Dialog
    if (showCoachingDialog) {
        var isCoachingOn by remember { mutableStateOf(coachingPrefs.isCoachingEnabled) }
        var announceKm by remember { mutableFloatStateOf(coachingPrefs.announcementFrequencyKm) }
        var announceMin by remember { mutableIntStateOf(coachingPrefs.announcementFrequencyMinutes) }

        AlertDialog(
            onDismissRequest = { showCoachingDialog = false },
            title = { Text("Voice Coaching Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spoken Walk Coaching", fontWeight = FontWeight.Bold)
                        Switch(checked = isCoachingOn, onCheckedChange = { isCoachingOn = it })
                    }

                    if (isCoachingOn) {
                        Text("Announcement Frequency", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = announceKm == 1.0f,
                                onClick = { announceKm = 1.0f },
                                label = { Text("Every 1 km") }
                            )
                            FilterChip(
                                selected = announceKm == 2.0f,
                                onClick = { announceKm = 2.0f },
                                label = { Text("Every 2 km") }
                            )
                            FilterChip(
                                selected = announceMin == 5,
                                onClick = { announceMin = 5 },
                                label = { Text("Every 5 min") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newPrefs = coachingPrefs.copy(
                        isCoachingEnabled = isCoachingOn,
                        announcementFrequencyKm = announceKm,
                        announcementFrequencyMinutes = announceMin
                    )
                    viewModel.saveVoiceCoachingPreferences(newPrefs)
                    showCoachingDialog = false
                    Toast.makeText(context, "Voice Coaching Saved!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCoachingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full Player Modal
    if (showFullPlayerSheet && currentTrack != null) {
        AlertDialog(
            onDismissRequest = { showFullPlayerSheet = false },
            title = { Text(currentTrack!!.title, fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(HealthNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = HealthTeal, modifier = Modifier.size(60.dp))
                    }

                    Text(currentTrack!!.artistOrAuthor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    val progress = if (totalDurationMs > 0) (currentPosMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
                    Slider(
                        value = currentPosMs.toFloat(),
                        onValueChange = { audioPlayerEngine.seekTo(it.toLong()) },
                        valueRange = 0f..totalDurationMs.coerceAtLeast(1000L).toFloat(),
                        colors = SliderDefaults.colors(thumbColor = HealthTeal, activeTrackColor = HealthTeal)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            val newSpeed = when (speed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            audioPlayerEngine.setSpeed(newSpeed)
                        }) {
                            Text("${speed}x", fontWeight = FontWeight.Bold, color = HealthTeal)
                        }

                        IconButton(onClick = { audioPlayerEngine.seekBackward(15000L) }) {
                            Icon(Icons.Default.FastRewind, contentDescription = "Rewind 15s")
                        }

                        IconButton(
                            onClick = {
                                if (isPlaying) audioPlayerEngine.pause() else audioPlayerEngine.resume()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(HealthTeal)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(onClick = { audioPlayerEngine.seekForward(15000L) }) {
                            Icon(Icons.Default.FastForward, contentDescription = "Forward 15s")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFullPlayerSheet = false }) {
                    Text("Close")
                }
            }
        )
    }
}
