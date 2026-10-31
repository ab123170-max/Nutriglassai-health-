package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.network.SupabaseClient
import com.example.ui.viewmodel.HealthViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun CloudBackupScreen(viewModel: HealthViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var signedIn by remember { mutableStateOf(SupabaseClient.hasSession(context)) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Your health data stays on this phone until you sign in and back it up.") }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("← Back to Profile") }
        Text("NutriGlass Cloud Backup", style = MaterialTheme.typography.headlineSmall)
        Text("Securely back up your health journey and restore it on another device.")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!signedIn) {
                    Text(if (isSignUp) "Create your account" else "Sign in to your account", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(email, { email = it }, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(password, { password = it }, label = { Text("Password (minimum 8 characters)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                    Row {
                        TextButton(onClick = { isSignUp = !isSignUp }) {
                            Text(if (isSignUp) "I already have an account" else "Create a new account")
                        }
                    }
                    Button(
                        enabled = !busy && email.contains("@") && password.length >= 8 && SupabaseClient.isConfigured,
                        onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    if (isSignUp) SupabaseClient.signUp(context, email.trim(), password)
                                    else SupabaseClient.signIn(context, email.trim(), password)
                                    signedIn = SupabaseClient.hasSession(context)
                                    status = if (signedIn) "Account connected." else "Check your email to confirm your account, then sign in."
                                } catch (e: Exception) {
                                    status = e.message ?: "Could not connect to your account."
                                }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isSignUp) "Create account" else "Sign in") }
                } else {
                    Text("Your account is connected on this device.", style = MaterialTheme.typography.titleMedium)
                    Button(
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            busy = true
                            viewModel.generateBackupJsonString { backup ->
                                scope.launch {
                                    try {
                                        val token = SupabaseClient.validAccessToken(context)
                                        SupabaseClient.upsertRecord(token, "full_backup_v1", "full_backup", JSONObject().put("backupJson", backup))
                                        status = "Cloud backup completed successfully."
                                    } catch (e: Exception) {
                                        status = e.message ?: "Backup failed."
                                    }
                                    busy = false
                                }
                            }
                        }
                    ) { Text("Back up my health data") }

                    Button(
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    val token = SupabaseClient.validAccessToken(context)
                                    val rows = JSONArray(SupabaseClient.fetchRecords(token, "full_backup"))
                                    if (rows.length() == 0) {
                                        status = "No cloud backup found for this account."
                                        busy = false
                                    } else {
                                        val json = rows.getJSONObject(0).optJSONObject("payload")?.optString("backupJson").orEmpty()
                                        if (json.isBlank()) {
                                            status = "The cloud backup is empty or unreadable."
                                            busy = false
                                        } else {
                                            viewModel.restoreDatabaseFromJson(json) { success ->
                                                status = if (success) "Restore completed." else "Restore failed."
                                                busy = false
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    status = e.message ?: "Restore failed."
                                    busy = false
                                }
                            }
                        }
                    ) { Text("Restore from cloud") }
                    TextButton(onClick = {
                        SupabaseClient.signOut(context)
                        signedIn = false
                        status = "Signed out on this device."
                    }) { Text("Sign out") }
                }
            }
        }
        if (busy) CircularProgressIndicator()
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("Your records are protected by account-level database security. This first release backs up health data JSON; photos are not included yet.", style = MaterialTheme.typography.bodySmall)
    }
}
