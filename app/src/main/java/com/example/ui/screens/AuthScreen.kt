package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.AuthManager
import com.example.data.repository.SkimmsRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: SkimmsRepository,
    onAdminLoginSuccess: () -> Unit,
    onWorkerLoginSuccess: (workerId: String, helmetNumber: String, workerName: String) -> Unit,
    onBackTo3dHelmet: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Worker Phone Login, 1 = Control Room Admin
    var workerIdInput by remember { mutableStateOf("ESP32_NODE_01") }
    var workerPinInput by remember { mutableStateOf("1234") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MineDarkBackground)
    ) {
        // Top Return to 3D Helmet button if callback provided
        if (onBackTo3dHelmet != null) {
            FilledTonalButton(
                onClick = onBackTo3dHelmet,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MineDarkSurface,
                    contentColor = SafetyAmber
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("3D Helmet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(SafetyAmber.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
                    .border(2.dp, SafetyAmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "SKIMMS Emblem",
                    tint = SafetyAmber,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SKIMMS",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 2.sp
            )
            Text(
                text = "Smart Kavach for Intelligent Mine Monitoring",
                fontSize = 13.sp,
                color = SafetyAmber,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Team HEXACREW • University of Calcutta • SIH26039",
                fontSize = 11.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Role Switcher Tab
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MineDarkSurfaceElevated,
                contentColor = SafetyAmber,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        errorMessage = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Worker View", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        errorMessage = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Control Room", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderSubtle, SafetyAmber.copy(alpha = 0.4f))))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedTab == 0) {
                        // Worker Login (ESP32 ID + PIN)
                        Text(
                            text = "Miner Authentication",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Access your assigned helmet telemetry & SOS alerts",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = workerIdInput,
                            onValueChange = { workerIdInput = it.uppercase() },
                            label = { Text("ESP32 Node ID") },
                            leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null, tint = SensorCyan) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("worker_id_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SensorCyan,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = workerPinInput,
                            onValueChange = { workerPinInput = it },
                            label = { Text("Worker 4-Digit PIN") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = SafetyAmber) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("worker_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafetyAmber,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Miner Presets for immediate testing
                        Text("Quick Demo Miners:", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilterChip(
                                selected = workerIdInput == "ESP32_NODE_01",
                                onClick = {
                                    workerIdInput = "ESP32_NODE_01"
                                    workerPinInput = "1234"
                                },
                                label = { Text("Ramesh (Node 1)", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = workerIdInput == "ESP32_NODE_02",
                                onClick = {
                                    workerIdInput = "ESP32_NODE_02"
                                    workerPinInput = "5678"
                                },
                                label = { Text("Subhash (Node 2)", fontSize = 10.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isAuthenticating = true
                                    errorMessage = null
                                    val worker = repository.getWorker(workerIdInput.trim())
                                    if (worker != null) {
                                        if (worker.workerPin == workerPinInput.trim()) {
                                            onWorkerLoginSuccess(worker.workerId, worker.helmetNumber, worker.workerName)
                                        } else {
                                            errorMessage = "Invalid 4-digit PIN for helmet ${worker.helmetNumber}."
                                        }
                                    } else {
                                        // Auto-provision demo worker for smooth prototype review if not exists
                                        if (workerIdInput == "ESP32_NODE_01" && workerPinInput == "1234") {
                                            onWorkerLoginSuccess("ESP32_NODE_01", "HLM-042", "Ramesh Kumar")
                                        } else {
                                            errorMessage = "Worker node '$workerIdInput' is not registered. Please register in Control Room."
                                        }
                                    }
                                    isAuthenticating = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("worker_login_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isAuthenticating && workerIdInput.isNotBlank() && workerPinInput.isNotBlank()
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enter Mine Station", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Admin Login with Google Sign-In via Credential Manager
                        Text(
                            text = "Control Room Supervisor",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Authenticate with Google to oversee all helmets, alerts & telemetry",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Button(
                            onClick = {
                                isAuthenticating = true
                                errorMessage = null
                                AuthManager.signInWithGoogle(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onSuccess = {
                                        isAuthenticating = false
                                        onAdminLoginSuccess()
                                    },
                                    onError = { err ->
                                        isAuthenticating = false
                                        errorMessage = err
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    },
                                    onCancelled = {
                                        isAuthenticating = false
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("admin_google_signin_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isAuthenticating
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fast Control Room Bypass for evaluation / offline testing
                        OutlinedButton(
                            onClick = {
                                onAdminLoginSuccess()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SensorCyan)
                        ) {
                            Text("Demo Control Room Bypass (Supervisor Access)", fontSize = 12.sp)
                        }
                    }

                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = msg,
                                color = StatusCritical,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
