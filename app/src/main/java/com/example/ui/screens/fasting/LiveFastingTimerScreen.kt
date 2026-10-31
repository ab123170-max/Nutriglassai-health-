package com.example.ui.screens.fasting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FastingViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveFastingTimerScreen(
    viewModel: FastingViewModel,
    onNavigateBack: () -> Unit
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    val currentProtocol = schedules.find { it.protocolId == profile?.selectedProtocolId } ?: schedules.firstOrNull()

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val elapsedSeconds = if (activeSession != null) (currentTime - activeSession!!.startTime) / 1000L else 0L
    val targetSeconds = currentProtocol?.let { it.fastingHours * 3600L } ?: (16 * 3600L)
    val progress = (elapsedSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Fasting Timer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentProtocol?.protocolName ?: "16:8 Fasting",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 14.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val seconds = elapsedSeconds % 60
                    Text(
                        text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target: ${currentProtocol?.fastingHours ?: 16}h",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (activeSession != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            viewModel.cancelOrPauseSession(activeSession!!.id, "CANCELLED")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).testTag("cancel_fast_btn")
                    ) {
                        Text("End / Stop Fast")
                    }

                    Button(
                        onClick = {
                            viewModel.endFastingSession(activeSession!!.id, elapsedSeconds / 60, 5, 8, "Completed successfully")
                        },
                        modifier = Modifier.weight(1f).testTag("complete_fast_btn")
                    ) {
                        Text("Complete Fast")
                    }
                }
            } else {
                Button(
                    onClick = {
                        viewModel.startFasting(currentProtocol?.protocolId ?: "16_8", currentProtocol?.fastingHours ?: 16)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("start_timer_btn")
                ) {
                    Text("Start Fasting Session")
                }
            }
        }
    }
}
