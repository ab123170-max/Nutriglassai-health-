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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingAnalyticsScreen(
    viewModel: FastingViewModel,
    onNavigateBack: () -> Unit
) {
    val progress by viewModel.latestProgress.collectAsState()

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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Fasting Progress & Consistency", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Consistency: ${progress?.fastingConsistencyPercentage ?: 85.0f}%", fontSize = 14.sp)
                        Text(text = "Average Duration: ${progress?.averageFastingDurationHours ?: 16.0f}h", fontSize = 14.sp)
                        Text(text = "Weight Trend: ${progress?.weightTrendKg ?: -0.5f} kg", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.generateWeeklyProgress(1) },
                            modifier = Modifier.fillMaxWidth().testTag("generate_weekly_report_btn")
                        ) {
                            Text("Generate Weekly Fasting Report")
                        }
                    }
                }
            }
        }
    }
}
