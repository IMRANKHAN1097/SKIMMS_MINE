package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiSafetyBoxEngine
import com.example.ai.SafetyEvaluationResult
import com.example.data.model.AlertRecord
import com.example.data.model.TelemetryData
import com.example.data.repository.SkimmsRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerPhoneScreen(
    repository: SkimmsRepository,
    workerId: String,
    helmetNumber: String,
    workerName: String,
    onSwitchUser: () -> Unit,
    onOpenDocs: () -> Unit,
    onOpenSimulator: () -> Unit,
    onOpen3dHelmet: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val safetyEngine = remember { AiSafetyBoxEngine() }

    // Live telemetry collected for this worker
    val telemetryList by repository.observeTelemetryForWorker(workerId).collectAsState(initial = emptyList())
    val alertsList by repository.observeAlerts().collectAsState(initial = emptyList())
    val workerAlerts = remember(alertsList, workerId) {
        alertsList.filter { it.workerId == workerId }
    }

    // Default telemetry if no live packet has arrived yet
    var currentTelemetry by remember {
        mutableStateOf(
            TelemetryData(
                workerId = workerId,
                helmetNumber = helmetNumber,
                mq4 = 380.0,
                mq135 = 261.0,
                accel = 9.8,
                ir = 49107L,
                batteryPct = 94,
                lat = 22.5726,
                lng = 88.3639,
                riskScore = 12,
                alertLevel = "SAFE"
            )
        )
    }

    // Update with latest packet
    LaunchedEffect(telemetryList) {
        if (telemetryList.isNotEmpty()) {
            currentTelemetry = telemetryList.first()
        }
    }

    // Evaluate live safety logic
    val safetyResult = remember(currentTelemetry) {
        safetyEngine.evaluate(currentTelemetry, currentTelemetry.timestampMillis)
    }

    // Trigger phone vibration when hazard level increases
    var lastAlertLevel by remember { mutableStateOf("SAFE") }
    LaunchedEffect(safetyResult.alertLevel) {
        if (safetyResult.alertLevel != "SAFE" && safetyResult.alertLevel != lastAlertLevel) {
            triggerHapticAlert(context)
        }
        lastAlertLevel = safetyResult.alertLevel
    }

    var showSosConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("SKIMMS Kavach", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MineDarkSurfaceHighlight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = helmetNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$workerName • Node $workerId",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onOpen3dHelmet?.invoke() ?: onSwitchUser() }) {
                        Icon(Icons.Default.Engineering, contentDescription = "3D Helmet & Sensors", tint = SafetyAmber)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSimulator,
                        modifier = Modifier.testTag("worker_open_simulator_button")
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = "Test Hardware Simulator", tint = SensorCyan)
                    }
                    IconButton(
                        onClick = onOpenDocs,
                        modifier = Modifier.testTag("worker_open_docs_button")
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Engineering Docs", tint = SafetyAmber)
                    }
                    IconButton(
                        onClick = onSwitchUser,
                        modifier = Modifier.testTag("worker_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Exit to login", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MineDarkSurface
                )
            )
        },
        bottomBar = {
            // Massive Floating SOS Action Bar
            Surface(
                color = MineDarkSurfaceElevated,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Button(
                    onClick = { showSosConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(60.dp)
                        .testTag("worker_sos_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("EMERGENCY SOS TRIGGER", fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        },
        containerColor = MineDarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // 1. BIG STATUS BLOCK
                BigStatusBlock(
                    statusLevel = safetyResult.alertLevel,
                    riskPercentage = safetyResult.riskScore,
                    reasons = safetyResult.reasons
                )
            }

            item {
                // 2. Physical Helmet Local Hardware State Mirror
                PhysicalHelmetMirrorCard(
                    ledState = safetyResult.ledState,
                    buzzerPeriodSec = safetyResult.buzzerPeriodSec,
                    batteryPct = currentTelemetry.batteryPct
                )
            }

            item {
                // 3. Sensor Quadrant Tiles
                Text(
                    text = "HELMET SENSOR ARRAY (ESP32 NODE)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SensorTile(
                        modifier = Modifier.weight(1f),
                        title = "MQ-4 Methane",
                        value = "${currentTelemetry.mq4.toInt()} ADC",
                        subtext = "Limit: 2200 ADC",
                        icon = Icons.Default.LocalFireDepartment,
                        statusColor = if (currentTelemetry.mq4 >= 2200) StatusCritical else if (currentTelemetry.mq4 >= 1500) StatusCaution else StatusSafe,
                        extraBadge = if (safetyResult.predictedGasSpikeIn60s) "▲ RISING" else null
                    )
                    SensorTile(
                        modifier = Modifier.weight(1f),
                        title = "MQ-135 Air",
                        value = "${currentTelemetry.mq135.toInt()} ADC",
                        subtext = "Limit: 2000 ADC",
                        icon = Icons.Default.Air,
                        statusColor = if (currentTelemetry.mq135 >= 2000) StatusCritical else if (currentTelemetry.mq135 >= 1400) StatusCaution else StatusSafe
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SensorTile(
                        modifier = Modifier.weight(1f),
                        title = "MPU6050 Accel",
                        value = "${String.format("%.1f", currentTelemetry.accel)} m/s²",
                        subtext = "Limit: ≥ 24.5 m/s²",
                        icon = Icons.Default.DirectionsRun,
                        statusColor = if (currentTelemetry.accel >= 24.5 || safetyResult.isManDown) StatusCritical else StatusSafe,
                        extraBadge = if (safetyResult.isManDown) "MAN DOWN" else null
                    )
                    SensorTile(
                        modifier = Modifier.weight(1f),
                        title = "MAX30105 Optical",
                        value = "${currentTelemetry.ir}",
                        subtext = "IR Pulse count",
                        icon = Icons.Default.Favorite,
                        statusColor = if (currentTelemetry.ir in 1000..20000) StatusCaution else StatusSafe
                    )
                }
            }

            item {
                // 4. GPS & Underground Mine Sector Location
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MineDarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SensorCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Underground Sector Location", fontSize = 12.sp, color = TextMuted)
                            Text("Sector 3 • Active Face Stope B", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "GPS: ${String.format("%.4f", currentTelemetry.lat)}° N, ${String.format("%.4f", currentTelemetry.lng)}° E",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Surface(
                            color = if (currentTelemetry.gpsValid) StatusSafe.copy(alpha = 0.2f) else StatusCaution.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (currentTelemetry.gpsValid) "GPS FIX" else "NO FIX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTelemetry.gpsValid) StatusSafe else StatusCaution,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            item {
                // 5. Worker Incident History
                Text(
                    text = "HELMET INCIDENT LOG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }

            if (workerAlerts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSafe, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No active alerts logged for this helmet.", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            } else {
                items(workerAlerts.take(5)) { alert ->
                    AlertFeedItem(alert = alert)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // SOS Confirmation Modal
    if (showSosConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TRIGGER SOS ALERT?", color = TextPrimary)
                }
            },
            text = {
                Text(
                    "This immediately broadcasts a CRITICAL emergency signal to the Mine Control Room, dispatches automated SMS/Telegram webhook notifications, and activates the helmet emergency buzzer.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmDialog = false
                        scope.launch {
                            val sosAlert = AlertRecord(
                                id = "alt_sos_${System.currentTimeMillis()}",
                                workerId = workerId,
                                workerName = workerName,
                                helmetNumber = helmetNumber,
                                alertLevel = "SOS",
                                reason = "MANUAL SOS TRIGGER: Miner pressed emergency distress button on helmet.",
                                riskScore = 100,
                                acknowledged = false,
                                escalated = false,
                                webhookDispatched = true,
                                lat = currentTelemetry.lat,
                                lng = currentTelemetry.lng,
                                createdAtMillis = System.currentTimeMillis()
                            )
                            repository.recordAlert(sosAlert)
                            triggerHapticAlert(context)
                            Toast.makeText(context, "SOS SIGNAL DISPATCHED TO CONTROL ROOM!", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    Text("BROADCAST SOS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = MineDarkSurfaceElevated
        )
    }
}

@Composable
fun BigStatusBlock(
    statusLevel: String,
    riskPercentage: Int,
    reasons: List<String>
) {
    val statusColor by animateColorAsState(
        targetValue = when (statusLevel) {
            "CRITICAL" -> StatusCritical
            "DANGER" -> StatusDanger
            "CAUTION" -> StatusCaution
            else -> StatusSafe
        },
        label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (statusLevel == "CRITICAL" || statusLevel == "DANGER") 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(statusColor, statusColor.copy(alpha = 0.3f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusLevel,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        letterSpacing = 1.sp
                    )
                }

                // Risk percentage circle badge
                Surface(
                    color = statusColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RISK ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text("$riskPercentage%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = statusColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Safety reasons summary
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                reasons.forEach { reason ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = statusColor, fontWeight = FontWeight.Bold)
                        Text(
                            text = reason,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhysicalHelmetMirrorCard(
    ledState: String,
    buzzerPeriodSec: Int,
    batteryPct: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HELMET HARDWARE STATUS (FAIL-SAFE MIRROR)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (batteryPct > 50) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = if (batteryPct > 20) SensorCyan else StatusCritical,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$batteryPct%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // LED Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ledColor = when {
                        ledState.startsWith("RED") -> StatusCritical
                        ledState.startsWith("YELLOW") -> StatusCaution
                        else -> StatusSafe
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(ledColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LED: $ledState",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                // Buzzer Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (buzzerPeriodSec > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = if (buzzerPeriodSec > 0) StatusCritical else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (buzzerPeriodSec > 0) "Buzzer: ${buzzerPeriodSec}s beep" else "Buzzer: Muted",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (buzzerPeriodSec > 0) StatusCritical else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun SensorTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    statusColor: Color,
    extraBadge: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderSubtle, statusColor.copy(alpha = 0.5f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                extraBadge?.let { badge ->
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(title, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Text(subtext, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
fun AlertFeedItem(alert: AlertRecord) {
    val alertColor = when (alert.alertLevel) {
        "CRITICAL", "SOS" -> StatusCritical
        "DANGER" -> StatusDanger
        else -> StatusCaution
    }
    val timeFormatted = remember(alert.createdAtMillis) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(alert.createdAtMillis))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(alertColor)
                    .padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${alert.alertLevel} - ${alert.helmetNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = alertColor
                    )
                    Text(timeFormatted, fontSize = 11.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(alert.reason, fontSize = 12.sp, color = TextPrimary)
            }
        }
    }
}

fun triggerHapticAlert(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(500)
        }
    } catch (e: Exception) {
        // Vibrator optional on emulator
    }
}
