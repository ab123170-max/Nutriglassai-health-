package com.example.ui.screens.fasting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FastingViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingAnalyticsScreen(
    viewModel: FastingViewModel,
    onNavigateBack: () -> Unit
) {
    val analytics by viewModel.weeklyAnalytics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fasting Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Last 7 Days — Actual Sessions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = analytics.consistencyPercentage?.let {
                                "Consistency: ${String.format(Locale.getDefault(), "%.1f", it)}%"
                            } ?: "Consistency: Not enough session data yet",
                            fontSize = 14.sp
                        )
                        Text(
                            text = analytics.averageDurationHours?.let {
                                "Average completed fast: ${String.format(Locale.getDefault(), "%.1f", it)} hours"
                            } ?: "Average completed fast: No completed sessions yet",
                            fontSize = 14.sp
                        )
                        Text("Completed sessions: ${analytics.completedSessions}", fontSize = 14.sp)
                        Text("Missed sessions: ${analytics.missedSessions}", fontSize = 14.sp)
                        Text(
                            text = "Weight trend: Track verified weight check-ins in Progress. Fasting sessions alone cannot determine weight change.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
