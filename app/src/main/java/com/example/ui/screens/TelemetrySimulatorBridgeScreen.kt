package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiSafetyBoxEngine
import com.example.data.model.AlertRecord
import com.example.data.model.TelemetryData
import com.example.data.repository.SkimmsRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetrySimulatorBridgeScreen(
    repository: SkimmsRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val safetyEngine = remember { AiSafetyBoxEngine() }

    var selectedWorkerId by remember { mutableStateOf("ESP32_NODE_01") }
    var selectedHelmetNumber by remember { mutableStateOf("HLM-042") }
    var selectedWorkerName by remember { mutableStateOf("Ramesh Kumar") }

    // Adjustable Sensor Telemetry Controls
    var mq4Val by remember { mutableFloatStateOf(380f) }
    var mq135Val by remember { mutableFloatStateOf(261f) }
    var accelVal by remember { mutableFloatStateOf(9.8f) }
    var irVal by remember { mutableLongStateOf(49107L) }
    var batteryVal by remember { mutableIntStateOf(94) }

    var mpuOk by remember { mutableStateOf(true) }
    var maxOk by remember { mutableStateOf(true) }
    var mq4Ok by remember { mutableStateOf(true) }
    var mq135Ok by remember { mutableStateOf(true) }
    var missingCount by remember { mutableIntStateOf(0) }

    var isLiveStreaming by remember { mutableStateOf(false) }
    var lastDispatchedJson by remember { mutableStateOf<String?>(null) }

    // Real-time evaluation
    val currentEval = remember(mq4Val, mq135Val, accelVal, irVal, missingCount, mpuOk, maxOk, mq4Ok, mq135Ok) {
        val tel = TelemetryData(
            workerId = selectedWorkerId,
            helmetNumber = selectedHelmetNumber,
            mq4 = mq4Val.toDouble(),
            mq135 = mq135Val.toDouble(),
            accel = accelVal.toDouble(),
            ir = irVal,
            mpuOk = mpuOk,
            maxOk = maxOk,
            mq4Ok = mq4Ok,
            mq135Ok = mq135Ok,
            missing = missingCount,
            batteryPct = batteryVal
        )
        safetyEngine.evaluate(tel)
    }

    // Auto stream loop (every 500ms when toggled)
    LaunchedEffect(isLiveStreaming) {
        while (isLiveStreaming) {
            val tel = TelemetryData(
                workerId = selectedWorkerId,
                helmetNumber = selectedHelmetNumber,
                mq4 = mq4Val.toDouble(),
                mq135 = mq135Val.toDouble(),
                accel = accelVal.toDouble(),
                ir = irVal,
                mpuOk = mpuOk,
                maxOk = maxOk,
                mq4Ok = mq4Ok,
                mq135Ok = mq135Ok,
                missing = missingCount,
                batteryPct = batteryVal,
                riskScore = currentEval.riskScore,
                alertLevel = currentEval.alertLevel,
                aiReasons = currentEval.reasons.joinToString("; ")
            )
            repository.submitTelemetry(tel)

            // Check if alert escalation required
            if (currentEval.alertLevel != "SAFE") {
                val alert = AlertRecord(
                    id = "alt_${selectedWorkerId}_${System.currentTimeMillis()}",
                    workerId = selectedWorkerId,
                    workerName = selectedWorkerName,
                    helmetNumber = selectedHelmetNumber,
                    alertLevel = currentEval.alertLevel,
                    reason = currentEval.reasons.firstOrNull() ?: "Hazard detected",
                    riskScore = currentEval.riskScore,
                    acknowledged = false,
                    escalated = false,
                    webhookDispatched = true,
                    createdAtMillis = System.currentTimeMillis()
                )
                repository.recordAlert(alert)
            }

            lastDispatchedJson = """
                {"board_id":"$selectedWorkerId","gps":{"valid":true,"lat":22.57,"lng":88.36},
                 "sensors":{"mq4":${mq4Val.toInt()},"mq135":${mq135Val.toInt()},"accel":${String.format("%.2f", accelVal)},"ir":$irVal},
                 "status":{"mpu_ok":$mpuOk,"max_ok":$maxOk,"mq4_ok":$mq4Ok,"mq135_ok":$mq135Ok,
                           "health_hazard":${currentEval.alertLevel == "CRITICAL" || currentEval.alertLevel == "DANGER"},
                           "env_hazard":${mq4Val >= 2200 || mq135Val >= 2000},"missing":$missingCount}}
            """.trimIndent().replace("\n", "")

            delay(500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ESP32 Telemetry Test Bench", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("Team HEXACREW Hardware & Fault Injector", fontSize = 11.sp, color = SafetyAmber)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { isLiveStreaming = !isLiveStreaming },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isLiveStreaming) StatusCritical else StatusSafe,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            if (isLiveStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isLiveStreaming) "Streaming (500ms)" else "Start WiFi Stream", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MineDarkSurface)
            )
        },
        containerColor = MineDarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Live Evaluator Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MineDarkSurfaceElevated),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LIVE SAFETY EVALUATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Surface(
                                color = when (currentEval.alertLevel) {
                                    "CRITICAL" -> StatusCritical
                                    "DANGER" -> StatusDanger
                                    "CAUTION" -> StatusCaution
                                    else -> StatusSafe
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${currentEval.alertLevel} (${currentEval.riskScore}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hardware LED: ${currentEval.ledState} | Buzzer: ${if (currentEval.buzzerPeriodSec > 0) "${currentEval.buzzerPeriodSec}s beep" else "Silent"}",
                            fontSize = 12.sp,
                            color = SensorCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        currentEval.reasons.forEach { reason ->
                            Text("• $reason", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            item {
                // Presets Grid
                Text(
                    text = "STEP-BY-STEP TEST TABLE SCENARIOS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafetyAmber,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetButton(
                            title = "1. Normal Clear",
                            subtitle = "MQ4: 380, Accel: 9.8",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                mq4Val = 380f
                                mq135Val = 261f
                                accelVal = 9.8f
                                irVal = 49107L
                                mpuOk = true; maxOk = true; mq4Ok = true; mq135Ok = true; missingCount = 0
                            }
                        )
                        PresetButton(
                            title = "2. Methane Spike",
                            subtitle = "MQ4: 2350 (≥ 2200)",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                mq4Val = 2350f
                                mq135Val = 310f
                                accelVal = 9.8f
                                irVal = 49107L
                            }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetButton(
                            title = "3. Toxic Air Spike",
                            subtitle = "MQ135: 2150 (≥ 2000)",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                mq4Val = 420f
                                mq135Val = 2150f
                                accelVal = 9.8f
                            }
                        )
                        PresetButton(
                            title = "4. Gas Rising Trend",
                            subtitle = "+60/s slope warning",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                mq4Val = 1950f
                                mq135Val = 320f
                            }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetButton(
                            title = "5. Fall / Impact",
                            subtitle = "Accel: 27.5 m/s²",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                accelVal = 27.5f
                            }
                        )
                        PresetButton(
                            title = "6. Man-Down",
                            subtitle = "Impact + Immobility",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                accelVal = 9.8f
                                scope.launch {
                                    // Trigger impact then immobility
                                    accelVal = 28.0f
                                    delay(400)
                                    accelVal = 9.8f
                                }
                            }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetButton(
                            title = "7. Weak Pulse",
                            subtitle = "IR: 8500 count",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                irVal = 8500L
                            }
                        )
                        PresetButton(
                            title = "8. Sensor Failure",
                            subtitle = "MPU6050 offline",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                mpuOk = false
                                missingCount = 1
                            }
                        )
                    }
                }
            }

            item {
                // Interactive Sensor Sliders
                Text(
                    text = "MANUAL ADC & SENSOR TELEMETRY SLIDERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("MQ-4 Methane: ${mq4Val.toInt()} ADC (Hazard ≥ 2200)", fontSize = 12.sp, color = TextPrimary)
                        Slider(
                            value = mq4Val,
                            onValueChange = { mq4Val = it },
                            valueRange = 100f..3500f,
                            colors = SliderDefaults.colors(thumbColor = SafetyAmber, activeTrackColor = SafetyAmber)
                        )

                        Text("MQ-135 Air Quality: ${mq135Val.toInt()} ADC (Hazard ≥ 2000)", fontSize = 12.sp, color = TextPrimary)
                        Slider(
                            value = mq135Val,
                            onValueChange = { mq135Val = it },
                            valueRange = 100f..3000f,
                            colors = SliderDefaults.colors(thumbColor = SensorCyan, activeTrackColor = SensorCyan)
                        )

                        Text("MPU6050 Acceleration: ${String.format("%.1f", accelVal)} m/s² (Impact ≥ 24.5)", fontSize = 12.sp, color = TextPrimary)
                        Slider(
                            value = accelVal,
                            onValueChange = { accelVal = it },
                            valueRange = 0f..40f,
                            colors = SliderDefaults.colors(thumbColor = StatusCritical, activeTrackColor = StatusCritical)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val tel = TelemetryData(
                                            workerId = selectedWorkerId,
                                            helmetNumber = selectedHelmetNumber,
                                            mq4 = mq4Val.toDouble(),
                                            mq135 = mq135Val.toDouble(),
                                            accel = accelVal.toDouble(),
                                            ir = irVal,
                                            mpuOk = mpuOk,
                                            maxOk = maxOk,
                                            mq4Ok = mq4Ok,
                                            mq135Ok = mq135Ok,
                                            missing = missingCount,
                                            riskScore = currentEval.riskScore,
                                            alertLevel = currentEval.alertLevel,
                                            aiReasons = currentEval.reasons.joinToString("; ")
                                        )
                                        repository.submitTelemetry(tel)
                                        Toast.makeText(context, "Single packet dispatched for $selectedHelmetNumber", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber, contentColor = Color.Black)
                            ) {
                                Text("Send 1 Telemetry Packet", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                // Raw JSON Preview
                Text(
                    text = "SERIAL / HTTP TELEMETRY PAYLOAD (EVERY 500MS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastDispatchedJson ?: """{"board_id":"$selectedWorkerId","gps":{"valid":true,"lat":22.57,"lng":88.36},"sensors":{"mq4":${mq4Val.toInt()},"mq135":${mq135Val.toInt()},"accel":${String.format("%.2f", accelVal)},"ir":$irVal},"status":{"mpu_ok":$mpuOk,"max_ok":$maxOk,"mq4_ok":$mq4Ok,"mq135_ok":$mq135Ok,"health_hazard":false,"env_hazard":false,"missing":$missingCount}}""",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SensorCyan,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PresetButton(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MineDarkSurfaceElevated,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}
