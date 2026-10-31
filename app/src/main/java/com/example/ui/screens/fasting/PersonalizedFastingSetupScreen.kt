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
fun PersonalizedFastingSetupScreen(
    viewModel: FastingViewModel,
    onNavigateBack: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()

    var ageText by remember(profile) { mutableStateOf(profile?.age?.toString() ?: "30") }
    var weightText by remember(profile) { mutableStateOf(profile?.weightKg?.toString() ?: "70") }
    var heightText by remember(profile) { mutableStateOf(profile?.heightCm?.toString() ?: "170") }
    var optedOut by remember(profile) { mutableStateOf(profile?.fastingOptedOut ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalized Fasting Setup") },
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Body & Health Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text("Age (years)") },
                            modifier = Modifier.fillMaxWidth().testTag("setup_age_input")
                        )

                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Weight (kg)") },
                            modifier = Modifier.fillMaxWidth().testTag("setup_weight_input")
                        )

                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("Height (cm)") },
                            modifier = Modifier.fillMaxWidth().testTag("setup_height_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opt out of fasting (balanced meals)", modifier = Modifier.weight(1f))
                            Switch(
                                checked = optedOut,
                                onCheckedChange = { optedOut = it },
                                modifier = Modifier.testTag("setup_opt_out_switch")
                            )
                        }

                        Button(
                            onClick = {
                                profile?.let { p ->
                                    viewModel.updateProfile(
                                        p.copy(
                                            age = ageText.toIntOrNull() ?: p.age,
                                            weightKg = weightText.toFloatOrNull() ?: p.weightKg,
                                            heightCm = heightText.toFloatOrNull() ?: p.heightCm,
                                            fastingOptedOut = optedOut
                                        )
                                    )
                                }
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_setup_btn")
                        ) {
                            Text("Save Fasting Profile")
                        }
                    }
                }
            }
        }
    }
}
