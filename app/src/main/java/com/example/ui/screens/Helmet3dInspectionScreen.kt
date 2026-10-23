package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.TelemetryData
import com.example.data.repository.SkimmsRepository
import com.example.ui.theme.*

data class SensorHardwareInfo(
    val id: String,
    val name: String,
    val shortCode: String,
    val category: String,
    val physicalLocation: String,
    val pinout: String,
    val busType: String,
    val workingVoltage: String,
    val sensingRange: String,
    val eyeWickZone: String,
    val safetyRule: String,
    val liveMetricKey: String,
    val accentColor: Color
)

val HELMET_SENSORS_CATALOG = listOf(
    SensorHardwareInfo(
        id = "mq4",
        name = "MQ-4 Methane Gas Sensor",
        shortCode = "CH4",
        category = "Gas & Explosives",
        physicalLocation = "Upper Brow Ridge / Visor Flange",
        pinout = "VCC (5V Boost) • GND • AOUT (GPIO34 ADC) • DOUT (NC)",
        busType = "Analog ADC (12-bit, 0-4095)",
        workingVoltage = "5.0V DC (Internal heater 150mA)",
        sensingRange = "300 - 10,000 ppm CH4 (Limit: 2200)",
        eyeWickZone = "Breather Eyelet Grommet with ePTFE hydrophobic gas-permeable membrane. Slurry must not touch heater coil.",
        safetyRule = "Threshold >= 2200 triggers Immediate Red Gas Hazard & fast buzzer (2s period).",
        liveMetricKey = "380 ppm",
        accentColor = GasWarningRed
    ),
    SensorHardwareInfo(
        id = "mq135",
        name = "MQ-135 Air Quality Sensor",
        shortCode = "AIR",
        category = "Hazardous Atmosphere",
        physicalLocation = "Lateral Breathing Zone Intake (Left Flange)",
        pinout = "VCC (5V Boost) • GND • AOUT (GPIO35 ADC) • DOUT (NC)",
        busType = "Analog ADC (12-bit, 0-4095)",
        workingVoltage = "5.0V DC (Internal heater 130mA)",
        sensingRange = "10 - 1000 ppm (CO2, NH3, Benzene, NOx)",
        eyeWickZone = "Lateral splash-deflector eyelet with dual Viton O-rings to stop capillary slurry intrusion.",
        safetyRule = "Threshold >= 2000 triggers Gas Hazard. Combined with health hazard triggers CRITICAL.",
        liveMetricKey = "261 ppm",
        accentColor = SafetyAmber
    ),
    SensorHardwareInfo(
        id = "mpu6050",
        name = "MPU-6050 6-Axis Gyro & Accel",
        shortCode = "IMU",
        category = "Motion & Impact",
        physicalLocation = "Rigid Helmet Crown Apex (Under protective dome)",
        pinout = "VCC (3.3V) • GND • SCL (GPIO22) • SDA (GPIO21) • INT (GPIO19)",
        busType = "I2C Bus (Addr: 0x68, 400kHz, 4.7kΩ pull-ups)",
        workingVoltage = "3.3V DC (Ultra-low 3.8mA)",
        sensingRange = "±16g / ±2000°/s (Impact limit: 24.5 m/s²)",
        eyeWickZone = "No crown drilling allowed! Bonded with 3M VHB 5952 acrylic foam tape. Wires routed through non-wicking perimeter harness.",
        safetyRule = "Impact >= 24.5 m/s² followed by 15s immobility triggers Man-Down CRITICAL alarm.",
        liveMetricKey = "9.8 m/s²",
        accentColor = SensorCyan
    ),
    SensorHardwareInfo(
        id = "max30105",
        name = "MAX30105 Optical Biosensor",
        shortCode = "HR/SpO2",
        category = "Physiological Vitals",
        physicalLocation = "Inner Forehead Sweatband Cradle (Direct skin contact)",
        pinout = "VIN (3.3V) • GND • SCL (GPIO22) • SDA (GPIO21) • INT (NC)",
        busType = "I2C Bus (Addr: 0x57, shared with MPU6050)",
        workingVoltage = "3.3V DC (LED supply 3.3V/5V)",
        sensingRange = "Optical Red 660nm & IR 940nm photoplethysmogram",
        eyeWickZone = "Medical-grade liquid silicone optical seal gasket. Stops acidic sweat salts from wicking into solder pads.",
        safetyRule = "IR count 1000 - 20000 indicates weak contact or hypoperfusion. 0 = sensor fallen off.",
        liveMetricKey = "49,107 IR",
        accentColor = PulsePink
    ),
    SensorHardwareInfo(
        id = "gps",
        name = "NEO-6M GPS & Patch Antenna",
        shortCode = "GPS",
        category = "Geospatial Telemetry",
        physicalLocation = "Top Apex Crest (Skyward zenith orientation)",
        pinout = "VCC (3.3V) • GND • TX (GPIO16 RX2) • RX (GPIO17 TX2)",
        busType = "UART2 (9600 baud, NMEA-0183 sentences)",
        workingVoltage = "3.3V DC (Active ceramic patch LNA)",
        sensingRange = "50-channel tracking, -161 dBm sensitivity",
        eyeWickZone = "Top ceramic antenna potted with transparent silicone. RF coax feed shielded through sealed nylon gland.",
        safetyRule = "Underground drift entry transitions smoothly from GPS to RF beacon stope localization.",
        liveMetricKey = "22.57°N, 88.36°E",
        accentColor = SuccessGreen
    ),
    SensorHardwareInfo(
        id = "dht11",
        name = "DHT11 Temp & Humidity Sensor",
        shortCode = "ENV",
        category = "Mine Stope Microclimate",
        physicalLocation = "Right Lateral Slotted Louver (Ambient drift airflow)",
        pinout = "VCC (3.3V/5V) • DATA (GPIO4 / 1-Wire) • GND",
        busType = "Custom Single-Wire Bidirectional Protocol",
        workingVoltage = "3.3V - 5.0V DC (1.5mA)",
        sensingRange = "0-50°C (±2°C), 20-90% RH (±5% RH)",
        eyeWickZone = "Downward drip loop eyelet prevents mine tunnel ceiling condensation droplets from entering sensing grid.",
        safetyRule = "Heat stress index alert when Temp > 35°C and Humidity > 85% RH.",
        liveMetricKey = "28.5°C • 65% RH",
        accentColor = SafetyAmber
    )
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Helmet3dInspectionScreen(
    repository: SkimmsRepository,
    onNavigateToWorkerPhone: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToMine3D: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    onNavigateToRescue: () -> Unit,
    onNavigateToDocs: () -> Unit
) {
    var selectedSensorId by remember { mutableStateOf("mq4") }
    var showEyeWickGuide by remember { mutableStateOf(false) }
    var showHardwarePhotoDialog by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isAutoRotate by remember { mutableStateOf(true) }

    val activeTelemetry by repository.observeActiveTelemetry().collectAsState(initial = TelemetryData())
    val selectedSensor = HELMET_SENSORS_CATALOG.firstOrNull { it.id == selectedSensorId } ?: HELMET_SENSORS_CATALOG[0]

    val threeJsHelmetHtml = remember {
        generate3DHelmetHtml()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SafetyAmber.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SafetyAmber),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Engineering,
                                    contentDescription = "SKIMMS Helmet",
                                    tint = SafetyAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "SKIMMS 3D SMART KAVACH",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "6 SENSORS ACTIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "Real-Resolution 3D Helmet • Physical Eye-Wick & Cable Harness",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showEyeWickGuide = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SafetyAmber.copy(alpha = 0.2f),
                            contentColor = SafetyAmber
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("eye_wick_guide_button")
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eye-Wick Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            isAutoRotate = !isAutoRotate
                            webViewRef?.evaluateJavascript("toggleAutoRotate($isAutoRotate)", null)
                        }
                    ) {
                        Icon(
                            if (isAutoRotate) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Toggle 3D Rotation",
                            tint = if (isAutoRotate) SafetyAmber else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MineDarkSurface)
            )
        },
        bottomBar = {
            // High-Utility Action Hub
            Surface(
                color = MineDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SYSTEM DISPATCH CONSOLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafetyAmber
                        )
                        Text(
                            "SIH26039 • Team HEXACREW",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onNavigateToWorkerPhone,
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber, contentColor = Color.Black),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_worker_phone_view"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Worker Phone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onNavigateToAdmin,
                            colors = ButtonDefaults.buttonColors(containerColor = MineCardDark, contentColor = TextPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_admin_control_room"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp), tint = SafetyAmber)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admin Room", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onNavigateToMine3D,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MineCardDark, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = "3D Mine Twin", tint = SensorCyan, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = onNavigateToSimulator,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MineCardDark, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = "Hardware Simulator", tint = SafetyAmber, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = onNavigateToRescue,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MineCardDark, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.MedicalServices, contentDescription = "Rescue Maps", tint = GasWarningRed, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = onNavigateToDocs,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MineCardDark, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "Engineering Docs", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        containerColor = MineDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive 3D WebGL Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onSensorClicked(sensorId: String) {
                                    post {
                                        selectedSensorId = sensorId
                                    }
                                }
                            }, "AndroidBridge")
                            loadDataWithBaseURL("https://threejs.org/", threeJsHelmetHtml, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 3D Canvas HUD Overlays
                // Top Left: Quick Camera Presets
                Surface(
                    color = MineDarkSurface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "RESET" to "resetView()",
                            "FRONT" to "focusView('front')",
                            "CROWN" to "focusView('crown')",
                            "TEMPLE" to "focusView('temple')",
                            "REAR" to "focusView('rear')"
                        ).forEach { (label, jsCommand) ->
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (label == "RESET") SafetyAmber else TextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        webViewRef?.evaluateJavascript(jsCommand, null)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Top Right: Live Sensor Real Photo Button
                FilledTonalButton(
                    onClick = { showHardwarePhotoDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MineDarkSurface.copy(alpha = 0.92f),
                        contentColor = SafetyAmber
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafetyAmber.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Real Hardware Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Bottom Center: 3D Interaction Hint
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        "Touch & drag to orbit 360° • Pinch to zoom • Tap any sensor node",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            // Sensor Selector Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MineDarkSurface)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HELMET_SENSORS_CATALOG) { sensor ->
                    val isSelected = sensor.id == selectedSensorId
                    val readingValue = when (sensor.id) {
                        "mq4" -> "${activeTelemetry.mq4.toInt()} ppm"
                        "mq135" -> "${activeTelemetry.mq135.toInt()} ppm"
                        "mpu6050" -> String.format("%.1f m/s²", activeTelemetry.accel)
                        "max30105" -> "${activeTelemetry.ir} IR"
                        "gps" -> if (activeTelemetry.gpsValid) "LOCKED" else "NO FIX"
                        "dht11" -> "${activeTelemetry.tempC.toInt()}°C • ${activeTelemetry.humidityPct.toInt()}%"
                        else -> "OK"
                    }

                    Surface(
                        color = if (isSelected) sensor.accentColor.copy(alpha = 0.18f) else MineCardDark,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) sensor.accentColor else BorderSubtle
                        ),
                        modifier = Modifier
                            .clickable {
                                selectedSensorId = sensor.id
                                webViewRef?.evaluateJavascript("highlightSensor('${sensor.id}')", null)
                            }
                            .testTag("sensor_chip_${sensor.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(sensor.accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    sensor.shortCode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) sensor.accentColor else TextPrimary
                                )
                                Text(
                                    readingValue,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Detailed Selected Sensor Technical Dossier & Eye-Wick Mounting Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MineCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, selectedSensor.accentColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = selectedSensor.accentColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        selectedSensor.shortCode,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = selectedSensor.accentColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        selectedSensor.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        selectedSensor.category,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Focus in 3D Button
                            FilledTonalButton(
                                onClick = {
                                    webViewRef?.evaluateJavascript("highlightSensor('${selectedSensor.id}')", null)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = selectedSensor.accentColor.copy(alpha = 0.2f),
                                    contentColor = selectedSensor.accentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Focus 3D", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Key Technical Specification Grid
                    item {
                        Surface(
                            color = MineDarkSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DossierRow("Physical Helmet Location", selectedSensor.physicalLocation, Icons.Default.Place, SafetyAmber)
                                DossierRow("Electrical Pinout & Bus", selectedSensor.pinout, Icons.Default.Cable, SensorCyan)
                                DossierRow("Bus Architecture", "${selectedSensor.busType} • ${selectedSensor.workingVoltage}", Icons.Default.Memory, TextPrimary)
                                DossierRow("Dynamic Sensing Range", selectedSensor.sensingRange, Icons.Default.Straighten, TextSecondary)
                            }
                        }
                    }

                    // Eye-Wick & Cable Gland Installation Note
                    item {
                        Surface(
                            color = SafetyAmber.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SafetyAmber.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.WaterDrop,
                                    contentDescription = "Eye-Wick Sealing",
                                    tint = SafetyAmber,
                                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "PHYSICAL EYE-WICK & SLURRY SEALING REQUIREMENT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafetyAmber
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        selectedSensor.eyeWickZone,
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Firmware & Safety Rule
                    item {
                        Surface(
                            color = MineDarkSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = "Safety Logic",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "LOCAL FIRMWARE v2.5.1 AUTONOMOUS SAFETY LOGIC",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        selectedSensor.safetyRule,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Real Hardware Photos Dialog
    if (showHardwarePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showHardwarePhotoDialog = false },
            confirmButton = {
                TextButton(onClick = { showHardwarePhotoDialog = false }) {
                    Text("Close Inspection", color = SafetyAmber)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = SafetyAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Real Hardware Sensors & Helmet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Bench-level high-resolution view of the 6 physical electronic breakout modules and the assembled industrial mining helmet:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // Real Hardware Breakout Array Image
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column {
                                Image(
                                    painter = painterResource(id = R.drawable.img_sensors_hardware_1792754021441),
                                    contentDescription = "Physical Sensor Breakout Modules",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                                Surface(
                                    color = MineDarkSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Fig 1: Benchtop modules: MQ-4, MQ-135, MPU6050, MAX30105, NEO-6M, DHT11 with labeled pins",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Assembled 3D Smart Helmet Image
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column {
                                Image(
                                    painter = painterResource(id = R.drawable.img_smart_helmet_3d_1792754004931),
                                    contentDescription = "Assembled Smart Helmet",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                                Surface(
                                    color = MineDarkSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Fig 2: Assembled Smart Kavach Helmet with safety shell, headlamp, wiring harness, and rear battery bay",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = MineCardDark
        )
    }

    // Modal: Comprehensive Physical Eye-Wicking & Assembly Blueprint Guide
    if (showEyeWickGuide) {
        AlertDialog(
            onDismissRequest = { showEyeWickGuide = false },
            confirmButton = {
                Button(
                    onClick = { showEyeWickGuide = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber, contentColor = Color.Black)
                ) {
                    Text("Done / Close Guide", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = SafetyAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Physical Eye-Wick & Assembly Blueprint", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SafetyAmber.copy(alpha = 0.5f))
                        ) {
                            Column {
                                Image(
                                    painter = painterResource(id = R.drawable.img_eye_wick_assembly_1792754037683),
                                    contentDescription = "Technical Eye-Wick Blueprint",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                                Surface(
                                    color = MineDarkSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Fig 3: Technical Blueprint: Eye-wick cable glands, stress relief loops, potting zones, and rear ESP32 enclosure",
                                        fontSize = 10.sp,
                                        color = SafetyAmber,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            color = MineDarkSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "WHAT IS 'EYE-WICKING' IN MINING SAFETY?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SafetyAmber
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "In deep underground coal/metal mines, relative humidity reaches 85-98% and conductive coal dust is suspended in slurry. Multi-strand copper wire acts like a candle wick: capillary action draws moisture and acidic mine water along the copper strands through cable sheathing directly into the sealed electronics enclosure and sensor solder joints. Physical 'Eye-Wicking' is the engineering technique of creating hermetic barriers using non-wicking silicone leads, double O-ring compression glands, marine-grade adhesive heat shrink, and neutral-cure RTV potting.",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "WHICH PORTIONS NEED PHYSICAL EYE-WICKING ON THE HELMET?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SensorCyan
                        )
                    }

                    item {
                        EyeWickPortionCard(
                            portionName = "1. Rear Nape Enclosure Cable Entry",
                            hardwareRequired = "M12 IP68 Brass/Nylon Cable Gland with Double Neoprene O-Rings",
                            instruction = "Where all 6 sensor harness bundles converge into the rear ESP32 & 18650 battery box. Tighten gland compression nut to 2.5 N·m and apply 3mm bead of RTV-162 non-corrosive silicone inside the junction.",
                            color = GasWarningRed
                        )
                    }

                    item {
                        EyeWickPortionCard(
                            portionName = "2. MQ-4 & MQ-135 Gas Chamber Breather Eyelets",
                            hardwareRequired = "Perforated Brass Eyelet Grommets with ePTFE Hydrophobic Membrane",
                            instruction = "Mount gas sensors inside downward-angled visor chambers. The 0.2 µm ePTFE membrane permits methane (0.55 density) and toxic gases to diffuse freely while completely blocking liquid slurry from wicking into the 900°C platinum heater coil.",
                            color = SafetyAmber
                        )
                    }

                    item {
                        EyeWickPortionCard(
                            portionName = "3. Crown Apex (MPU-6050 & NEO GPS)",
                            hardwareRequired = "Zero Structural Drilling! 3M VHB 5952 Acrylic Foam Tape + Perimeter Guide",
                            instruction = "DO NOT drill holes through the crown apex (drilling voids ANSI Z89.1 / EN 397 impact rating). Secure sensor carrier using high-shear 3M VHB tape and route 26 AWG non-wicking silicone wires through the side rim accessory slots.",
                            color = SensorCyan
                        )
                    }

                    item {
                        EyeWickPortionCard(
                            portionName = "4. Inner Forehead MAX30105 Optical Window",
                            hardwareRequired = "Medical-Grade Liquid Silicone Rubber (LSR) Optical Gasket",
                            instruction = "Isolates the optical biosensor lenses from corrosive human sweat salts. Maintains optical clarity at 660nm (Red) and 940nm (IR) against skin while preventing sweat from wicking into solder eyelets.",
                            color = PulsePink
                        )
                    }

                    item {
                        EyeWickPortionCard(
                            portionName = "5. DHT11 Lateral Ventilation Labyrinth",
                            hardwareRequired = "3D-Printed Baffle with Downward-Facing Condensation Drip Loop",
                            instruction = "Enclose DHT11 in a 45° louvered vent. Add a 10mm U-bend drip loop in the 1-wire cable before it enters the eyelet grommet so condensation runs off rather than wicking toward the pins.",
                            color = SuccessGreen
                        )
                    }

                    item {
                        Surface(
                            color = MineDarkSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "STEP-BY-STEP PHYSICAL ASSEMBLY PROCEDURE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SafetyAmber
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "1. Use 26 AWG ultra-flexible silicone wire with tin-plated copper strands.\n" +
                                    "2. Solder all connections with 63/37 leaded or Sn96.5Ag3Cu0.5 lead-free solder; clean flux residue with 99% IPA.\n" +
                                    "3. Slide 3:1 dual-wall polyolefin heat shrink tubing with internal hot-melt adhesive over every eyelet.\n" +
                                    "4. Solder 4.7kΩ pull-up resistors on the shared I2C bus (SDA GPIO21, SCL GPIO22) at the ESP32 motherboard.\n" +
                                    "5. Mount the 5V Boost converter (MT3608) for MQ-4/MQ-135 heaters with a 220µF low-ESR buffer capacitor.\n" +
                                    "6. Seal rear enclosure with IP65 silicone perimeter gasket. Bench-test all 6 telemetry channels for 30 minutes before mine dispatch.",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            },
            containerColor = MineCardDark
        )
    }
}

@Composable
fun DossierRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("$label: ", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EyeWickPortionCard(portionName: String, hardwareRequired: String, instruction: String, color: Color) {
    Surface(
        color = MineDarkSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(portionName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
            Text("Hardware: $hardwareRequired", fontSize = 10.sp, color = SafetyAmber, fontWeight = FontWeight.SemiBold)
            Text(instruction, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}

/**
 * Three.js WebGL procedural 3D Smart Kavach Mining Helmet with all 6 sensors,
 * wire harnesses, eye-wick cable glands, and interactive orbit controls.
 */
fun generate3DHelmetHtml(): String {
    return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <title>SKIMMS 3D Smart Kavach Helmet</title>
    <style>
        body, html {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            overflow: hidden;
            background-color: #0d0f12;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            user-select: none;
            -webkit-user-select: none;
        }
        #canvas-container {
            width: 100%;
            height: 100%;
            position: absolute;
            top: 0;
            left: 0;
        }
        .hud-overlay {
            position: absolute;
            pointer-events: none;
            z-index: 10;
        }
        .sensor-tag {
            position: absolute;
            transform: translate(-50%, -50%);
            background: rgba(18, 22, 28, 0.85);
            border: 1px solid rgba(255, 179, 0, 0.7);
            border-radius: 4px;
            padding: 3px 6px;
            font-size: 10px;
            font-weight: bold;
            color: #ffffff;
            white-space: nowrap;
            pointer-events: auto;
            cursor: pointer;
            box-shadow: 0 2px 8px rgba(0,0,0,0.5);
            transition: all 0.2s ease;
        }
        .sensor-tag:active {
            transform: translate(-50%, -50%) scale(1.1);
            background: rgba(255, 179, 0, 0.9);
            color: #000;
        }
        .sensor-tag.active-tag {
            border-color: #00e5ff;
            background: rgba(0, 229, 255, 0.25);
            color: #00e5ff;
            box-shadow: 0 0 10px rgba(0, 229, 255, 0.6);
        }
    </style>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js"></script>
</head>
<body>
    <div id="canvas-container"></div>
    <div id="labels-container" class="hud-overlay" style="width:100%; height:100%;"></div>

    <script>
        let scene, camera, renderer, controls;
        let helmetGroup;
        let autoRotate = true;
        const sensorObjects = {};
        const sensorPositions = {
            mq4: new THREE.Vector3(0, 0.6, 2.05),
            mq135: new THREE.Vector3(-1.8, 0.1, 0.7),
            mpu6050: new THREE.Vector3(0, 2.05, 0.1),
            max30105: new THREE.Vector3(0, -0.15, 1.8),
            gps: new THREE.Vector3(0, 2.35, -0.6),
            dht11: new THREE.Vector3(1.85, 0.1, 0.7)
        };
        const sensorLabels = {
            mq4: "MQ-4 Methane",
            mq135: "MQ-135 Air",
            mpu6050: "MPU6050 IMU",
            max30105: "MAX30105 Vitals",
            gps: "NEO GPS",
            dht11: "DHT11 Temp/RH"
        };
        const labelElements = {};

        function init() {
            const container = document.getElementById('canvas-container');
            const width = container.clientWidth || window.innerWidth;
            const height = container.clientHeight || window.innerHeight;

            scene = new THREE.Scene();
            scene.fog = new THREE.FogExp2(0x0d0f12, 0.04);

            camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100);
            camera.position.set(4.5, 3.2, 5.5);

            renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
            renderer.setSize(width, height);
            renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
            renderer.toneMapping = THREE.ACESFilmicToneMapping;
            renderer.toneMappingExposure = 1.25;
            renderer.shadowMap.enabled = true;
            renderer.shadowMap.type = THREE.PCFSoftShadowMap;
            container.appendChild(renderer.domElement);

            controls = new THREE.OrbitControls(camera, renderer.domElement);
            controls.enableDamping = true;
            controls.dampingFactor = 0.05;
            controls.maxDistance = 14;
            controls.minDistance = 2.0;
            controls.target.set(0, 0.6, 0);

            // Lighting Rig
            const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
            scene.add(ambientLight);

            const keyLight = new THREE.DirectionalLight(0xfffaed, 1.6);
            keyLight.position.set(5, 8, 6);
            keyLight.castShadow = true;
            scene.add(keyLight);

            const fillLight = new THREE.DirectionalLight(0x4080ff, 0.8);
            fillLight.position.set(-6, -2, -4);
            scene.add(fillLight);

            const rimLight = new THREE.DirectionalLight(0xffaa00, 1.2);
            rimLight.position.set(0, 5, -8);
            scene.add(rimLight);

            // Create Subterranean Pedestal / Workbench
            const floorGeo = new THREE.CylinderGeometry(4.2, 4.4, 0.25, 48);
            const floorMat = new THREE.MeshStandardMaterial({
                color: 0x181c22,
                roughness: 0.85,
                metalness: 0.2
            });
            const floorMesh = new THREE.Mesh(floorGeo, floorMat);
            floorMesh.position.y = -1.2;
            floorMesh.receiveShadow = true;
            scene.add(floorMesh);

            const ringGeo = new THREE.RingGeometry(3.6, 3.8, 48);
            const ringMat = new THREE.MeshBasicMaterial({ color: 0xffb300, side: THREE.DoubleSide });
            const ringMesh = new THREE.Mesh(ringGeo, ringMat);
            ringMesh.rotation.x = -Math.PI / 2;
            ringMesh.position.y = -1.06;
            scene.add(ringMesh);

            buildSmartHelmet();
            createHUDLabels();

            window.addEventListener('resize', onWindowResize, false);
            container.addEventListener('pointerdown', () => { autoRotate = false; });

            animate();
        }

        function buildSmartHelmet() {
            helmetGroup = new THREE.Group();
            scene.add(helmetGroup);

            // Materials
            const helmetMat = new THREE.MeshStandardMaterial({
                color: 0xf59e0b, // Safety Orange/Yellow
                roughness: 0.3,
                metalness: 0.15,
                clearcoat: 0.3
            });

            const brimMat = new THREE.MeshStandardMaterial({
                color: 0xd97706,
                roughness: 0.4,
                metalness: 0.1
            });

            const retroTapeMat = new THREE.MeshStandardMaterial({
                color: 0xe2e8f0,
                emissive: 0x475569,
                roughness: 0.2,
                metalness: 0.7
            });

            const blackPlasticMat = new THREE.MeshStandardMaterial({
                color: 0x1e293b,
                roughness: 0.7,
                metalness: 0.1
            });

            const metallicSteelMat = new THREE.MeshStandardMaterial({
                color: 0xcfd8dc,
                roughness: 0.2,
                metalness: 0.85
            });

            const pcbBlueMat = new THREE.MeshStandardMaterial({
                color: 0x0284c7,
                roughness: 0.4,
                metalness: 0.3
            });

            const pcbPurpleMat = new THREE.MeshStandardMaterial({
                color: 0x7c3aed,
                roughness: 0.4,
                metalness: 0.3
            });

            // 1. Helmet Dome (Upper Half Sphere stretched along Z)
            const domeGeo = new THREE.SphereGeometry(2.0, 32, 24, 0, Math.PI * 2, 0, Math.PI / 2);
            domeGeo.scale(1.0, 0.95, 1.25);
            const domeMesh = new THREE.Mesh(domeGeo, helmetMat);
            domeMesh.castShadow = true;
            domeMesh.receiveShadow = true;
            helmetGroup.add(domeMesh);

            // 2. Helmet Crown Ridge (Reinforced Impact Rib)
            const ribGeo = new THREE.BoxGeometry(0.35, 0.4, 2.7);
            const ribMesh = new THREE.Mesh(ribGeo, helmetMat);
            ribMesh.position.set(0, 1.85, 0);
            ribMesh.rotation.x = -0.05;
            helmetGroup.add(ribMesh);

            // 3. Protective Brim
            const brimGeo = new THREE.TorusGeometry(2.15, 0.22, 16, 48, Math.PI * 2);
            brimGeo.scale(1.0, 0.4, 1.35);
            const brimMesh = new THREE.Mesh(brimGeo, brimMat);
            brimMesh.rotation.x = Math.PI / 2;
            brimMesh.position.y = -0.05;
            helmetGroup.add(brimMesh);

            // Extended Visor Peak at Front
            const visorGeo = new THREE.CylinderGeometry(2.35, 2.45, 0.12, 32, 1, false, Math.PI * 0.25, Math.PI * 0.5);
            const visorMesh = new THREE.Mesh(visorGeo, brimMat);
            visorMesh.position.set(0, -0.05, 0.4);
            helmetGroup.add(visorMesh);

            // 4. Retroreflective Safety Bands
            const tapeGeo = new THREE.CylinderGeometry(2.02, 2.06, 0.18, 32, 1, true);
            tapeGeo.scale(1.0, 1.0, 1.26);
            const tapeMesh = new THREE.Mesh(tapeGeo, retroTapeMat);
            tapeMesh.position.y = 0.55;
            helmetGroup.add(tapeMesh);

            // 5. High-Power LED Miner Headlamp
            const lampHousingGeo = new THREE.CylinderGeometry(0.35, 0.42, 0.5, 24);
            const lampHousing = new THREE.Mesh(lampHousingGeo, blackPlasticMat);
            lampHousing.rotation.x = Math.PI / 2;
            lampHousing.position.set(0, 0.75, 2.45);
            helmetGroup.add(lampHousing);

            const lampBezelGeo = new THREE.TorusGeometry(0.38, 0.05, 12, 24);
            const lampBezel = new THREE.Mesh(lampBezelGeo, metallicSteelMat);
            lampBezel.position.set(0, 0.75, 2.7);
            helmetGroup.add(lampBezel);

            const lensGeo = new THREE.CircleGeometry(0.33, 24);
            const lensMat = new THREE.MeshBasicMaterial({ color: 0xffffff });
            const lensMesh = new THREE.Mesh(lensGeo, lensMat);
            lensMesh.position.set(0, 0.75, 2.71);
            helmetGroup.add(lensMesh);

            // Headlamp Forward Spotlight Cone
            const headlampSpot = new THREE.SpotLight(0xfff7e6, 2.5, 15, Math.PI / 6, 0.4, 1.5);
            headlampSpot.position.set(0, 0.75, 2.75);
            headlampSpot.target.position.set(0, 0.2, 10);
            scene.add(headlampSpot);
            scene.add(headlampSpot.target);

            // 6. Rear Nape Electronics Enclosure (ESP32 + Battery + Eye-Wick Glands)
            const encGeo = new THREE.BoxGeometry(1.4, 1.1, 0.7);
            const encMesh = new THREE.Mesh(encGeo, blackPlasticMat);
            encMesh.position.set(0, 0.45, -2.4);
            helmetGroup.add(encMesh);

            // Gland 1 & 2 (Brass Eye-Wick Cable Glands)
            const glandGeo = new THREE.CylinderGeometry(0.12, 0.14, 0.35, 12);
            const brassMat = new THREE.MeshStandardMaterial({ color: 0xd4af37, metalness: 0.8, roughness: 0.3 });

            const gland1 = new THREE.Mesh(glandGeo, brassMat);
            gland1.rotation.z = Math.PI / 2;
            gland1.position.set(-0.75, 0.35, -2.35);
            helmetGroup.add(gland1);

            const gland2 = new THREE.Mesh(glandGeo, brassMat);
            gland2.rotation.z = -Math.PI / 2;
            gland2.position.set(0.75, 0.35, -2.35);
            helmetGroup.add(gland2);

            // ================= SENSORS BUILD =================

            // SENSOR 1: MQ-4 Methane Gas Sensor (Visor Brow Ridge)
            const mq4Group = new THREE.Group();
            mq4Group.position.copy(sensorPositions.mq4);

            const mq4BaseGeo = new THREE.CylinderGeometry(0.3, 0.32, 0.15, 20);
            const mq4Base = new THREE.Mesh(mq4BaseGeo, blackPlasticMat);
            mq4Group.add(mq4Base);

            const mq4MeshGeo = new THREE.CylinderGeometry(0.24, 0.24, 0.3, 20);
            const meshWireMat = new THREE.MeshStandardMaterial({
                color: 0x94a3b8,
                metalness: 0.9,
                roughness: 0.25,
                wireframe: false
            });
            const mq4MeshCap = new THREE.Mesh(mq4MeshGeo, meshWireMat);
            mq4MeshCap.position.y = 0.2;
            mq4Group.add(mq4MeshCap);

            // Cap rim ring
            const capRing = new THREE.Mesh(new THREE.TorusGeometry(0.24, 0.03, 8, 20), metallicSteelMat);
            capRing.rotation.x = Math.PI / 2;
            capRing.position.y = 0.35;
            mq4Group.add(capRing);

            helmetGroup.add(mq4Group);
            sensorObjects.mq4 = mq4Group;

            // SENSOR 2: MQ-135 Air Quality Sensor (Lateral Breathing Intake Port - Left)
            const mq135Group = new THREE.Group();
            mq135Group.position.copy(sensorPositions.mq135);
            mq135Group.rotation.y = -Math.PI / 3;

            const mq135Pcb = new THREE.Mesh(new THREE.BoxGeometry(0.55, 0.1, 0.7), pcbPurpleMat);
            mq135Group.add(mq135Pcb);

            const mq135Cap = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.22, 0.28, 18), meshWireMat);
            mq135Cap.position.y = 0.18;
            mq135Group.add(mq135Cap);

            helmetGroup.add(mq135Group);
            sensorObjects.mq135 = mq135Group;

            // SENSOR 3: MPU6050 6-Axis Gyro & Accel (Rigid Crown Apex)
            const mpuGroup = new THREE.Group();
            mpuGroup.position.copy(sensorPositions.mpu6050);

            const mpuPcb = new THREE.Mesh(new THREE.BoxGeometry(0.55, 0.06, 0.75), pcbBlueMat);
            mpuGroup.add(mpuPcb);

            // Black QFN chip
            const chipGeo = new THREE.BoxGeometry(0.22, 0.08, 0.22);
            const chipMesh = new THREE.Mesh(chipGeo, blackPlasticMat);
            chipMesh.position.set(0, 0.04, 0);
            mpuGroup.add(chipMesh);

            // Gold header pins
            const headerPins = new THREE.Mesh(new THREE.BoxGeometry(0.48, 0.12, 0.08), brassMat);
            headerPins.position.set(0, 0.05, 0.3);
            mpuGroup.add(headerPins);

            helmetGroup.add(mpuGroup);
            sensorObjects.mpu6050 = mpuGroup;

            // SENSOR 4: MAX30105 Optical Biosensor (Inner Forehead Sweatband Cradle)
            const maxGroup = new THREE.Group();
            maxGroup.position.copy(sensorPositions.max30105);

            const maxPcb = new THREE.Mesh(new THREE.BoxGeometry(0.45, 0.06, 0.45), blackPlasticMat);
            maxGroup.add(maxPcb);

            // Dual optical lenses (Red & IR)
            const redLens = new THREE.Mesh(new THREE.SphereGeometry(0.06, 12, 12), new THREE.MeshBasicMaterial({ color: 0xef4444 }));
            redLens.position.set(-0.1, 0.05, 0);
            maxGroup.add(redLens);

            const irLens = new THREE.Mesh(new THREE.SphereGeometry(0.06, 12, 12), new THREE.MeshBasicMaterial({ color: 0xec4899 }));
            irLens.position.set(0.1, 0.05, 0);
            maxGroup.add(irLens);

            helmetGroup.add(maxGroup);
            sensorObjects.max30105 = maxGroup;

            // SENSOR 5: NEO-6M GPS Module & Patch Antenna (Top Apex Crest)
            const gpsGroup = new THREE.Group();
            gpsGroup.position.copy(sensorPositions.gps);

            // Ceramic patch (tan/beige ceramic cube)
            const patchMat = new THREE.MeshStandardMaterial({ color: 0xd6c4b2, roughness: 0.6, metalness: 0.1 });
            const patchGeo = new THREE.BoxGeometry(0.65, 0.14, 0.65);
            const patchMesh = new THREE.Mesh(patchGeo, patchMat);
            gpsGroup.add(patchMesh);

            // Silver electrode center pin
            const feedPin = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 0.16, 12), metallicSteelMat);
            feedPin.position.set(0, 0.04, 0);
            gpsGroup.add(feedPin);

            // Underlying receiver PCB
            const gpsPcb = new THREE.Mesh(new THREE.BoxGeometry(0.8, 0.05, 0.8), pcbBlueMat);
            gpsPcb.position.y = -0.1;
            gpsGroup.add(gpsPcb);

            helmetGroup.add(gpsGroup);
            sensorObjects.gps = gpsGroup;

            // SENSOR 6: DHT11 Temp & Humidity Sensor (Lateral Right Vent Louver)
            const dhtGroup = new THREE.Group();
            dhtGroup.position.copy(sensorPositions.dht11);
            dhtGroup.rotation.y = Math.PI / 3;

            // Iconic blue slotted plastic casing
            const dhtMat = new THREE.MeshStandardMaterial({ color: 0x38bdf8, roughness: 0.35, metalness: 0.1 });
            const dhtBody = new THREE.Mesh(new THREE.BoxGeometry(0.48, 0.65, 0.25), dhtMat);
            dhtGroup.add(dhtBody);

            // Slots on DHT11
            const slotMat = new THREE.MeshBasicMaterial({ color: 0x0369a1 });
            for (let i = -0.15; i <= 0.15; i += 0.1) {
                const slot = new THREE.Mesh(new THREE.BoxGeometry(0.32, 0.03, 0.26), slotMat);
                slot.position.y = i;
                dhtGroup.add(slot);
            }

            helmetGroup.add(dhtGroup);
            sensorObjects.dht11 = dhtGroup;

            // ================= PHYSICAL WIRE HARNESSES & EYE-WICK TUBES =================
            buildWireHarnesses();
        }

        function buildWireHarnesses() {
            // Harness 1: Left Flange Cable Bundle (MQ-135 + MPU6050 -> Rear Enclosure)
            createWireCurve([
                new THREE.Vector3(-1.8, 0.1, 0.7),
                new THREE.Vector3(-1.9, 0.05, -0.6),
                new THREE.Vector3(-1.4, 0.25, -1.8),
                new THREE.Vector3(-0.75, 0.35, -2.35)
            ], 0x0284c7, 0.035); // Cyan ADC/I2C

            // Harness 2: Right Flange Cable Bundle (DHT11 + MQ-4 -> Rear Enclosure)
            createWireCurve([
                new THREE.Vector3(1.85, 0.1, 0.7),
                new THREE.Vector3(1.9, 0.05, -0.6),
                new THREE.Vector3(1.4, 0.25, -1.8),
                new THREE.Vector3(0.75, 0.35, -2.35)
            ], 0xf59e0b, 0.035); // Amber Wire

            // Harness 3: Front Visor to Side Rim (MQ-4 Front Wire)
            createWireCurve([
                new THREE.Vector3(0, 0.6, 2.05),
                new THREE.Vector3(1.0, 0.2, 1.8),
                new THREE.Vector3(1.85, 0.1, 0.7)
            ], 0xef4444, 0.03); // Red Power & Analog

            // Harness 4: Top Crown Apex to Rear (GPS + IMU)
            createWireCurve([
                new THREE.Vector3(0, 2.2, -0.6),
                new THREE.Vector3(0, 1.7, -1.6),
                new THREE.Vector3(0, 0.8, -2.2)
            ], 0x10b981, 0.035); // Green Serial/UART
        }

        function createWireCurve(points, colorHex, radius) {
            const curve = new THREE.CatmullRomCurve3(points);
            const tubeGeo = new THREE.TubeGeometry(curve, 32, radius, 8, false);
            const tubeMat = new THREE.MeshStandardMaterial({
                color: colorHex,
                roughness: 0.5,
                metalness: 0.1
            });
            const tubeMesh = new THREE.Mesh(tubeGeo, tubeMat);
            helmetGroup.add(tubeMesh);
        }

        function createHUDLabels() {
            const container = document.getElementById('labels-container');
            container.innerHTML = '';

            Object.keys(sensorPositions).forEach(key => {
                const el = document.createElement('div');
                el.className = 'sensor-tag';
                el.id = 'tag-' + key;
                el.innerText = sensorLabels[key];
                el.onclick = () => {
                    highlightSensor(key);
                    if (window.AndroidBridge && window.AndroidBridge.onSensorClicked) {
                        window.AndroidBridge.onSensorClicked(key);
                    }
                };
                container.appendChild(el);
                labelElements[key] = el;
            });
        }

        function updateLabels() {
            if (!camera || !helmetGroup) return;

            const tempV = new THREE.Vector3();
            const widthHalf = (window.innerWidth) / 2;
            const heightHalf = (window.innerHeight) / 2;

            Object.keys(sensorPositions).forEach(key => {
                const el = labelElements[key];
                if (!el) return;

                const pos = sensorPositions[key].clone();
                pos.applyMatrix4(helmetGroup.matrixWorld);

                tempV.copy(pos);
                tempV.project(camera);

                // Is in front of camera?
                if (tempV.z < 1) {
                    const x = (tempV.x * widthHalf) + widthHalf;
                    const y = -(tempV.y * heightHalf) + heightHalf;
                    el.style.display = 'block';
                    el.style.left = x + 'px';
                    el.style.top = y + 'px';
                } else {
                    el.style.display = 'none';
                }
            });
        }

        function highlightSensor(sensorId) {
            autoRotate = false;

            // Remove previous active classes
            Object.keys(labelElements).forEach(k => {
                labelElements[k].classList.remove('active-tag');
            });

            if (labelElements[sensorId]) {
                labelElements[sensorId].classList.add('active-tag');
            }

            // Smooth camera lerp to target sensor
            const targetPos = sensorPositions[sensorId];
            if (!targetPos) return;

            const worldPos = targetPos.clone().applyMatrix4(helmetGroup.matrixWorld);
            controls.target.lerp(worldPos, 0.9);

            // Position camera offset based on sensor location
            let camOffset = new THREE.Vector3(0, 1.5, 4.0);
            if (sensorId === 'mq4') camOffset.set(0, 1.2, 4.8);
            if (sensorId === 'mq135') camOffset.set(-4.5, 1.2, 2.5);
            if (sensorId === 'mpu6050') camOffset.set(0, 4.8, 1.5);
            if (sensorId === 'max30105') camOffset.set(0, 0.4, 3.8);
            if (sensorId === 'gps') camOffset.set(0, 4.9, -1.8);
            if (sensorId === 'dht11') camOffset.set(4.5, 1.2, 2.5);

            camera.position.copy(worldPos.clone().add(camOffset));
        }

        function resetView() {
            autoRotate = true;
            controls.target.set(0, 0.6, 0);
            camera.position.set(4.5, 3.2, 5.5);
            Object.keys(labelElements).forEach(k => {
                labelElements[k].classList.remove('active-tag');
            });
        }

        function focusView(preset) {
            autoRotate = false;
            if (preset === 'front') {
                controls.target.set(0, 0.6, 0.5);
                camera.position.set(0, 1.2, 5.2);
            } else if (preset === 'crown') {
                controls.target.set(0, 1.5, 0);
                camera.position.set(0, 5.5, 0.6);
            } else if (preset === 'temple') {
                controls.target.set(-1.0, 0.4, 1.0);
                camera.position.set(-3.8, 1.4, 3.2);
            } else if (preset === 'rear') {
                controls.target.set(0, 0.5, -1.5);
                camera.position.set(0, 1.8, -5.5);
            }
        }

        function toggleAutoRotate(state) {
            autoRotate = state;
        }

        function onWindowResize() {
            const container = document.getElementById('canvas-container');
            const width = container.clientWidth || window.innerWidth;
            const height = container.clientHeight || window.innerHeight;
            camera.aspect = width / height;
            camera.updateProjectionMatrix();
            renderer.setSize(width, height);
        }

        function animate() {
            requestAnimationFrame(animate);

            if (autoRotate && helmetGroup) {
                helmetGroup.rotation.y += 0.006;
            }

            controls.update();
            updateLabels();
            renderer.render(scene, camera);
        }

        window.onload = init;
    </script>
</body>
</html>
    """.trimIndent()
}
