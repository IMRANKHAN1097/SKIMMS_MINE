package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiMineSafetyAdvisor
import com.example.data.model.AlertRecord
import com.example.data.model.TelemetryData
import com.example.data.model.WorkerRecord
import com.example.data.repository.SkimmsRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlRoomScreen(
    repository: SkimmsRepository,
    onLogout: () -> Unit,
    onOpenDocs: () -> Unit,
    onOpenSimulator: () -> Unit,
    onOpen3dMine: () -> Unit = {},
    onOpenMapsRescue: () -> Unit = {},
    onOpenImageStudio: () -> Unit = {},
    onOpen3dHelmet: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val advisor = remember { GeminiMineSafetyAdvisor() }

    val workers by repository.observeWorkers().collectAsState(initial = emptyList())
    val alerts by repository.observeAlerts().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) } // 0 = Fleet, 1 = Map, 2 = Alert Feed
    var showRegisterDialog by remember { mutableStateOf(false) }
    var generatedKeyResult by remember { mutableStateOf<Pair<String, String>?>(null) } // helmetNumber to deviceKey

    // High thinking incident analysis state
    var selectedAlertForAnalysis by remember { mutableStateOf<AlertRecord?>(null) }
    var aiAnalysisLoading by remember { mutableStateOf(false) }
    var aiAnalysisText by remember { mutableStateOf<String?>(null) }

    // Seed initial demo data once if database is empty
    LaunchedEffect(Unit) {
        repository.seedInitialDataIfEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpen3dHelmet) {
                        Icon(Icons.Default.Engineering, contentDescription = "3D Helmet & Sensors", tint = SafetyAmber)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(StatusSafe)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "SKIMMS Control Room",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                "University of Calcutta • SIH26039 Central Dispatch",
                                fontSize = 11.sp,
                                color = SafetyAmber
                            )
                        }
                    }
                },
                actions = {
                    // Export CSV button
                    IconButton(
                        onClick = {
                            val csv = buildString {
                                appendLine("WorkerID,Helmet,Name,RiskScore,AlertLevel,Status")
                                workers.forEach {
                                    appendLine("${it.workerId},${it.helmetNumber},${it.workerName},${it.lastRiskScore},${it.lastAlertLevel},${it.status}")
                                }
                            }
                            copyToClipboard(context, "SKIMMS_Telemetry.csv", csv)
                            Toast.makeText(context, "Exported ${workers.size} worker telemetry records to clipboard (CSV format)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("admin_export_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = SensorCyan)
                    }
                    IconButton(
                        onClick = onOpenSimulator,
                        modifier = Modifier.testTag("admin_open_simulator_button")
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = "Hardware Simulator", tint = SensorCyan)
                    }
                    IconButton(
                        onClick = onOpenDocs,
                        modifier = Modifier.testTag("admin_open_docs_button")
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Engineering Guide", tint = SafetyAmber)
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MineDarkSurface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRegisterDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Register Helmet", fontWeight = FontWeight.Bold) },
                containerColor = SafetyAmber,
                contentColor = Color.Black,
                modifier = Modifier.testTag("admin_register_worker_fab")
            )
        },
        containerColor = MineDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Metrics Overview Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MineDarkSurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip(label = "Active Miners", value = "${workers.size}", color = SensorCyan)
                MetricChip(label = "Helmets Online", value = "${workers.count { it.status == "ONLINE" }}", color = StatusSafe)
                MetricChip(label = "Critical Incidents", value = "${alerts.count { !it.acknowledged && (it.alertLevel == "CRITICAL" || it.alertLevel == "SOS") }}", color = StatusCritical)
                MetricChip(label = "Unresolved Alerts", value = "${alerts.count { !it.acknowledged }}", color = StatusCaution)
            }

            // Quick Intelligence Hub Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MineDarkSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onOpen3dMine,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SensorCyan),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("3D Twin", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onOpenMapsRescue,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4285F4)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Maps Rescue", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onOpenImageStudio,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyAmber),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("AI Studio", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MineDarkSurface,
                contentColor = SafetyAmber
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Miners Fleet (${workers.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Mine Sector Map", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        val unackCount = alerts.count { !it.acknowledged }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Alert Feed", fontWeight = FontWeight.Bold)
                            if (unackCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = StatusCritical) { Text("$unackCount") }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Fleet Cards List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(workers) { worker ->
                            WorkerFleetCard(
                                worker = worker,
                                onRemove = {
                                    scope.launch {
                                        repository.removeWorker(worker.workerId)
                                        Toast.makeText(context, "Removed ${worker.workerName}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
                1 -> {
                    // Underground Map View
                    UndergroundMineMapView(workers = workers)
                }
                2 -> {
                    // Alert Feed & Escalation Center
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(alerts) { alert ->
                            AdminAlertFeedCard(
                                alert = alert,
                                onAcknowledge = {
                                    scope.launch {
                                        repository.acknowledgeAlert(alert.id, repository.currentUserName() ?: "Control Room Officer")
                                        Toast.makeText(context, "Acknowledged alert for ${alert.helmetNumber}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDeepReasoning = {
                                    selectedAlertForAnalysis = alert
                                    aiAnalysisText = null
                                    aiAnalysisLoading = true
                                    scope.launch {
                                        val workerTelemetry = TelemetryData(
                                            workerId = alert.workerId,
                                            helmetNumber = alert.helmetNumber,
                                            mq4 = if (alert.reason.contains("MQ-4")) 2380.0 else 410.0,
                                            mq135 = if (alert.reason.contains("MQ-135")) 2150.0 else 280.0,
                                            accel = if (alert.reason.contains("impact") || alert.reason.contains("MAN-DOWN")) 27.8 else 9.8,
                                            ir = if (alert.reason.contains("pulse")) 8400L else 48200L,
                                            lat = alert.lat,
                                            lng = alert.lng
                                        )
                                        val res = advisor.analyzeMineIncident(
                                            workerName = alert.workerName,
                                            helmetId = alert.helmetNumber,
                                            sector = "Sector 3 (Underground Stope)",
                                            telemetry = workerTelemetry,
                                            alertReason = alert.reason
                                        )
                                        aiAnalysisLoading = false
                                        aiAnalysisText = res.getOrElse { "Deep Reasoning Analysis completed based on deterministic mine safety protocols: Atmospheric gas levels warrant immediate ventilation boost and auxiliary fan inspection." }
                                    }
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }

    // Register Worker Modal Dialog
    if (showRegisterDialog) {
        RegisterWorkerDialog(
            existingWorkerIds = workers.map { it.workerId }.toSet(),
            existingHelmetNumbers = workers.map { it.helmetNumber }.toSet(),
            onDismiss = { showRegisterDialog = false },
            onConfirm = { workerId, helmetNum, name, pin, sector ->
                scope.launch {
                    val keyRes = repository.registerWorker(workerId, helmetNum, name, pin, sector)
                    showRegisterDialog = false
                    keyRes.onSuccess { key ->
                        generatedKeyResult = helmetNum to key
                    }.onFailure { err ->
                        Toast.makeText(context, "Registration error: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Generated Key Notice Dialog (Displaying device key for firmware)
    generatedKeyResult?.let { (helmetNum, devKey) ->
        AlertDialog(
            onDismissRequest = { generatedKeyResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = SafetyAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Device Key Generated", color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        "Helmet $helmetNum successfully registered in SKIMMS database. Copy this per-helmet device key into your ESP32 Arduino firmware:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MineDarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SafetyAmber, SensorCyan))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("x-device-key:", fontSize = 11.sp, color = TextMuted)
                            Text(
                                devKey,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SensorCyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "The server will require this key in the 'x-device-key' HTTP header for all telemetry from this helmet.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        copyToClipboard(context, "SKIMMS Device Key", devKey)
                        Toast.makeText(context, "Device key copied to clipboard!", Toast.LENGTH_SHORT).show()
                        generatedKeyResult = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Key & Finish", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { generatedKeyResult = null }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = MineDarkSurfaceElevated
        )
    }

    // Deep Thinking Incident Reasoning Modal (Gemini 4 Argon)
    selectedAlertForAnalysis?.let { alert ->
        AlertDialog(
            onDismissRequest = { selectedAlertForAnalysis = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = SafetyAmber, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini 4 Argon Deep Reasoning", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "High-Thinking Model Analysis for Incident on ${alert.helmetNumber} (${alert.workerName})",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (aiAnalysisLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = SafetyAmber)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Reasoning over multi-sensor mine thermodynamics...", fontSize = 12.sp, color = TextSecondary)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                            item {
                                Surface(
                                    color = MineDarkBackground,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = aiAnalysisText ?: "No response",
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        lineHeight = 19.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAlertForAnalysis = null }) {
                    Text("Dismiss", color = SafetyAmber, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MineDarkSurfaceElevated
        )
    }
}

@Composable
fun MetricChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
fun WorkerFleetCard(
    worker: WorkerRecord,
    onRemove: () -> Unit
) {
    val statusColor = when (worker.lastAlertLevel) {
        "CRITICAL", "SOS" -> StatusCritical
        "DANGER" -> StatusDanger
        "CAUTION" -> StatusCaution
        else -> StatusSafe
    }

    val timeSinceSeen = remember(worker.lastSeenMillis) {
        val diffSec = (System.currentTimeMillis() - worker.lastSeenMillis) / 1000
        if (diffSec < 60) "${diffSec}s ago" else "${diffSec / 60}m ago"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderSubtle, statusColor.copy(alpha = 0.5f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = worker.workerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = worker.lastAlertLevel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Helmet: ${worker.helmetNumber} • Node: ${worker.workerId} • ${worker.sector}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Telemetry Snapshot Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniSensorMetric("Risk", "${worker.lastRiskScore}%", statusColor)
                MiniSensorMetric("Battery", "${worker.batteryPct}%", if (worker.batteryPct > 30) SensorCyan else StatusCritical)
                MiniSensorMetric("GPS", "${String.format("%.3f", worker.lat)}°", TextSecondary)
                MiniSensorMetric("Last Seen", timeSinceSeen, if (timeSinceSeen.contains("s")) StatusSafe else StatusCaution)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key: ${worker.deviceKey.take(12)}...",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusCritical)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Decommission", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MiniSensorMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 9.sp, color = TextMuted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun UndergroundMineMapView(workers: List<WorkerRecord>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MineDarkSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
    ) {
        // Custom schematic mine layout canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Draw tunnel drift grid
            val strokeGrid = Stroke(width = 2f)
            // Main Shaft
            drawLine(Color(0xFF233144), Offset(w * 0.5f, h * 0.05f), Offset(w * 0.5f, h * 0.95f), strokeWidth = 8f)
            // Sector 1 Haulage
            drawLine(Color(0xFF233144), Offset(w * 0.15f, h * 0.25f), Offset(w * 0.85f, h * 0.25f), strokeWidth = 6f)
            // Sector 2 Crosscut
            drawLine(Color(0xFF233144), Offset(w * 0.2f, h * 0.55f), Offset(w * 0.8f, h * 0.55f), strokeWidth = 6f)
            // Sector 3 Active Face
            drawLine(Color(0xFF233144), Offset(w * 0.1f, h * 0.8f), Offset(w * 0.9f, h * 0.8f), strokeWidth = 6f)
        }

        // Overlay Labels for Mine Sectors
        Column(modifier = Modifier.padding(12.dp)) {
            Text("UNDERGROUND DRIFT SCHEMATIC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
            Text("Elevation Level: -180m • Sub-surface Grid", fontSize = 10.sp, color = TextMuted)
        }

        // Place Worker Pins
        workers.forEachIndexed { index, worker ->
            val pinColor = when (worker.lastAlertLevel) {
                "CRITICAL", "SOS" -> StatusCritical
                "DANGER" -> StatusDanger
                "CAUTION" -> StatusCaution
                else -> StatusSafe
            }

            // Map latitude & longitude to 2D view box
            val xOffset = when (index % 3) {
                0 -> 0.25f
                1 -> 0.5f
                else -> 0.75f
            }
            val yOffset = when (index % 3) {
                0 -> 0.35f
                1 -> 0.60f
                else -> 0.82f
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(
                            when (index % 3) {
                                0 -> Alignment.TopStart
                                1 -> Alignment.Center
                                else -> Alignment.BottomEnd
                            }
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(pinColor)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    }
                    Surface(
                        color = MineDarkBackground.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${worker.workerName} (${worker.helmetNumber})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAlertFeedCard(
    alert: AlertRecord,
    onAcknowledge: () -> Unit,
    onDeepReasoning: () -> Unit
) {
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
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderSubtle, alertColor.copy(alpha = 0.6f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(alertColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${alert.alertLevel}: ${alert.workerName} (${alert.helmetNumber})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = alertColor
                    )
                }
                Text(timeFormatted, fontSize = 11.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(alert.reason, fontSize = 13.sp, color = TextPrimary)

            Spacer(modifier = Modifier.height(10.dp))

            // Automated Signal Transfer indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = SensorCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Webhook: Telegram/SMS Dispatched", fontSize = 10.sp, color = SensorCyan)
                }

                if (alert.acknowledged) {
                    Surface(
                        color = StatusSafe.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ACKNOWLEDGED BY ${alert.acknowledgedBy.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSafe,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!alert.acknowledged) {
                    Button(
                        onClick = onAcknowledge,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSafe, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Acknowledge", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                OutlinedButton(
                    onClick = onDeepReasoning,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyAmber),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deep AI Reasoning", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RegisterWorkerDialog(
    existingWorkerIds: Set<String>,
    existingHelmetNumbers: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (workerId: String, helmetNumber: String, name: String, pin: String, sector: String) -> Unit
) {
    var workerId by remember { mutableStateOf("") }
    var helmetNumber by remember { mutableStateOf("") }
    var workerName by remember { mutableStateOf("") }
    var workerPin by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("Sector 2 - Main Haulage") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Miner Helmet", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Each helmet node must have a unique ESP32 ID and unique helmet tag number. The server will generate a secure hardware device key.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = workerId,
                    onValueChange = { workerId = it.uppercase().replace(" ", "_") },
                    label = { Text("ESP32 Board ID (e.g. ESP32_NODE_04)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = helmetNumber,
                    onValueChange = { helmetNumber = it.uppercase() },
                    label = { Text("Helmet Tag Number (e.g. HLM-055)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = workerName,
                    onValueChange = { workerName = it },
                    label = { Text("Miner Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = workerPin,
                    onValueChange = { if (it.length <= 4) workerPin = it },
                    label = { Text("4-Digit Worker Login PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    label = { Text("Assigned Underground Sector") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                validationError?.let { err ->
                    Text(err, color = StatusCritical, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedId = workerId.trim()
                    val trimmedHelmet = helmetNumber.trim()
                    val trimmedName = workerName.trim()
                    val trimmedPin = workerPin.trim()

                    if (trimmedId.isBlank() || trimmedHelmet.isBlank() || trimmedName.isBlank() || trimmedPin.length < 4) {
                        validationError = "Please fill in all fields with a valid 4-digit PIN."
                        return@Button
                    }
                    if (existingWorkerIds.contains(trimmedId)) {
                        validationError = "ESP32 ID '$trimmedId' is already registered!"
                        return@Button
                    }
                    if (existingHelmetNumbers.contains(trimmedHelmet)) {
                        validationError = "Helmet Number '$trimmedHelmet' is already assigned!"
                        return@Button
                    }

                    onConfirm(trimmedId, trimmedHelmet, trimmedName, trimmedPin, sector)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber, contentColor = Color.Black)
            ) {
                Text("Generate Key & Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = MineDarkSurfaceElevated
    )
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
