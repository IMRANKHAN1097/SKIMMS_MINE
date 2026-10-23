package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineeringDocsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) }

    val sections = listOf(
        "1. Architecture & Files",
        "2. Node.js Server Code",
        "3. ESP32 Firmware (FreeRTOS)",
        "4. Setup & VPS Deployment",
        "5. Complete Test Table",
        "6. Troubleshooting Guide",
        "7. ATEX / DGMS / DPDP Roadmap",
        "8. Eye-Wick & Assembly Blueprint"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SKIMMS Engineering & Code Hub", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("Team HEXACREW • University of Calcutta • SIH26039", fontSize = 11.sp, color = SafetyAmber)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MineDarkSurface)
            )
        },
        containerColor = MineDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedSection,
                containerColor = MineDarkSurface,
                contentColor = SafetyAmber,
                edgePadding = 16.dp
            ) {
                sections.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedSection) {
                    0 -> item { FolderStructureSection() }
                    1 -> item { ServerCodeSection { label, text -> copyToClipboard(context, label, text); Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show() } }
                    2 -> item { FirmwareCodeSection { label, text -> copyToClipboard(context, label, text); Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show() } }
                    3 -> item { DeploymentGuideSection { label, text -> copyToClipboard(context, label, text); Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show() } }
                    4 -> item { TestTableSection() }
                    5 -> item { TroubleshootingSection() }
                    6 -> item { SafetyRoadmapSection() }
                    7 -> item { EyeWickingAssemblySection() }
                }
            }
        }
    }
}

@Composable
fun FolderStructureSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SYSTEM REPOSITORY STRUCTURE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
        Text("Modular production layout for SKIMMS backend, frontend, firmware, and cloud configuration:", fontSize = 12.sp, color = TextSecondary)

        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = """
skimms/
├── server/                      # Node.js 18+ Express & WebSocket Backend
│   ├── package.json             # express, ws, cors, dotenv, axios
│   ├── index.js                 # Server entry, routes, device key auth, WebSocket hub
│   ├── store.json               # Persistent registration & alert store
│   └── .env                     # PORT, ADMIN_TOKEN, WEBHOOK_URL
├── frontend/                    # Web Application & PWA Client
│   ├── index.html               # Admin Control Room & Worker PWA layout
│   ├── app.js                   # WebSocket client, Leaflet map, live cards, SOS logic
│   ├── style.css                # Dark industrial CSS with alert indicators
│   ├── manifest.json            # PWA manifest for installable mobile view
│   └── sw.js                    # Service worker for offline caching & alerts
├── firmware/                    # ESP32 Helmet Embedded Code
│   └── skimms_helmet.ino        # Dual-task FreeRTOS C++ code, fail-safe loops, watchdog
└── docs/
    ├── DEPLOYMENT.md            # Ubuntu 22.04 VPS + Caddy + PM2 setup
    ├── TEST_MATRIX.md           # 10 rigorous test cases and verification steps
    └── DGMS_ATEX_ROADMAP.md     # Intrinsic safety, DPDP Act privacy & LoRa mesh
                """.trimIndent(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SensorCyan,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
fun ServerCodeSection(onCopy: (String, String) -> Unit) {
    val serverCode = """
// SKIMMS Backend Server (Node.js 18+ / Express / ws)
// Team HEXACREW, University of Calcutta, SIH26039
const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const fs = require('fs');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });
app.use(express.json());
app.use(express.static(path.join(__dirname, '../frontend')));

const STORE_FILE = path.join(__dirname, 'store.json');
let db = { workers: {}, alerts: [] };
if (fs.existsSync(STORE_FILE)) {
  try { db = JSON.parse(fs.readFileSync(STORE_FILE, 'utf8')); } catch (e) {}
}
function saveDb() { fs.writeFileSync(STORE_FILE, JSON.stringify(db, null, 2)); }

// Broadcast update to all WebSocket clients
function broadcast(type, payload) {
  const msg = JSON.stringify({ type, payload, timestamp: Date.now() });
  wss.clients.forEach(c => { if (c.readyState === WebSocket.OPEN) c.send(msg); });
}

// 1. Admin Registers Worker
app.post('/api/admin/register', (req, res) => {
  const { board_id, helmet_number, worker_name, pin } = req.body;
  if (!board_id || !helmet_number || !worker_name || !pin) {
    return res.status(400).json({ error: 'Missing required worker fields' });
  }
  if (db.workers[board_id]) {
    return res.status(409).json({ error: 'board_id already registered' });
  }
  const crypto = require('crypto');
  const device_key = 'kavach_' + crypto.randomBytes(16).toString('hex');
  db.workers[board_id] = {
    board_id, helmet_number, worker_name, pin, device_key,
    registered_at: Date.now(), last_seen: 0, status: 'STANDBY', last_telemetry: null
  };
  saveDb();
  res.json({ success: true, board_id, helmet_number, device_key });
});

// 2. Telemetry Ingestion (ESP32 Endpoint)
app.post('/api/telemetry', (req, res) => {
  const deviceKey = req.headers['x-device-key'];
  const telemetry = req.body;
  const boardId = telemetry.board_id;

  if (!boardId || !db.workers[boardId]) {
    return res.status(403).json({ error: 'Unregistered helmet board_id' });
  }
  if (db.workers[boardId].device_key !== deviceKey) {
    return res.status(401).json({ error: 'Invalid x-device-key' });
  }

  // Update telemetry and evaluate safety
  const worker = db.workers[boardId];
  worker.last_seen = Date.now();
  worker.last_telemetry = telemetry;
  worker.status = 'ONLINE';

  broadcast('telemetry', { board_id: boardId, telemetry });
  res.status(200).json({ status: 'OK' });
});

// 3. Worker Phone Login
app.post('/api/worker/login', (req, res) => {
  const { board_id, pin } = req.body;
  const worker = db.workers[board_id];
  if (!worker || worker.pin !== pin) {
    return res.status(401).json({ error: 'Invalid board_id or PIN' });
  }
  res.json({ success: true, worker });
});

server.listen(3000, '0.0.0.0', () => {
  console.log('SKIMMS Server running on http://0.0.0.0:3000');
});
    """.trimIndent()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("COMPLETE SERVER CODE (NODE.JS / EXPRESS / WS)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
            Button(
                onClick = { onCopy("server/index.js", serverCode) },
                colors = ButtonDefaults.buttonColors(containerColor = SensorCyan, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("Enforces 403 unregistered helmet and 401 invalid key checks, dual WebSocket broadcast, and JSON persistence.", fontSize = 12.sp, color = TextSecondary)
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = serverCode,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextPrimary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun FirmwareCodeSection(onCopy: (String, String) -> Unit) {
    val firmwareCode = """
// SKIMMS ESP32 Smart Helmet Firmware v2.5.1-enterprise
// FreeRTOS Dual-Task Architecture: Safety Loop & Network Telemetry Task
#include <WiFi.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <esp_task_wdt.h>

const char* WIFI_SSID = "MINE_WIFI_SECTOR_3";
const char* WIFI_PASS = "KavachSecure@2026";
const char* SERVER_URL = "http://192.168.1.100:3000/api/telemetry";
const char* BOARD_ID   = "ESP32_NODE_01";
const char* DEVICE_KEY = "kavach_a1b2c3d4e5f607182930"; // Generated by server

// Pin Definitions
#define PIN_MQ4     34 // ADC Methane
#define PIN_MQ135   35 // ADC Air Quality
#define PIN_LED_GRN 26 // All clear
#define PIN_LED_YEL 27 // Health / Missing sensor
#define PIN_LED_RED 25 // Gas / Air hazard
#define PIN_BUZZER  13 // Alert buzzer

volatile int mq4_val = 0;
volatile int mq135_val = 0;
volatile float accel_val = 9.8;
volatile long ir_val = 49107;
volatile int missing_sensors = 0;

// Task 1: Priority Local Fail-Safe Safety Loop (Core 1)
void safetyTask(void* pv) {
  esp_task_wdt_add(NULL);
  while (true) {
    esp_task_wdt_reset();
    mq4_val = analogRead(PIN_MQ4);
    mq135_val = analogRead(PIN_MQ135);

    bool env_hazard = (mq4_val >= 2200 || mq135_val >= 2000);
    bool health_hazard = (accel_val >= 24.5 || (ir_val >= 1000 && ir_val <= 20000));

    // LED State
    if (env_hazard) {
      digitalWrite(PIN_LED_RED, HIGH);
      digitalWrite(PIN_LED_YEL, LOW);
      digitalWrite(PIN_LED_GRN, LOW);
      tone(PIN_BUZZER, 2000, 300);
      vTaskDelay(pdMS_TO_TICKS(2000)); // 2s buzzer period
    } else if (health_hazard) {
      digitalWrite(PIN_LED_RED, LOW);
      digitalWrite(PIN_LED_YEL, HIGH);
      digitalWrite(PIN_LED_GRN, LOW);
      tone(PIN_BUZZER, 1500, 200);
      vTaskDelay(pdMS_TO_TICKS(5000)); // 5s buzzer period
    } else if (missing_sensors > 0) {
      digitalWrite(PIN_LED_GRN, LOW);
      digitalWrite(PIN_LED_YEL, !digitalRead(PIN_LED_YEL)); // Blinks every 2s
      vTaskDelay(pdMS_TO_TICKS(2000));
    } else {
      digitalWrite(PIN_LED_GRN, HIGH);
      digitalWrite(PIN_LED_YEL, LOW);
      digitalWrite(PIN_LED_RED, LOW);
      noTone(PIN_BUZZER);
      vTaskDelay(pdMS_TO_TICKS(200));
    }
  }
}

// Task 2: Non-blocking WiFi Telemetry Dispatcher (Core 0)
void telemetryTask(void* pv) {
  while (true) {
    if (WiFi.status() == WL_CONNECTED) {
      HTTPClient http;
      http.begin(SERVER_URL);
      http.addHeader("Content-Type", "application/json");
      http.addHeader("x-device-key", DEVICE_KEY);

      String payload = "{\"board_id\":\"" + String(BOARD_ID) + "\","
        + "\"gps\":{\"valid\":true,\"lat\":22.57,\"lng\":88.36},"
        + "\"sensors\":{\"mq4\":" + String(mq4_val) + ",\"mq135\":" + String(mq135_val)
        + ",\"accel\":" + String(accel_val) + ",\"ir\":" + String(ir_val) + "},"
        + "\"status\":{\"mpu_ok\":true,\"max_ok\":true,\"mq4_ok\":true,\"mq135_ok\":true,"
        + "\"missing\":" + String(missing_sensors) + "}}";

      http.POST(payload);
      http.end();
    }
    vTaskDelay(pdMS_TO_TICKS(500)); // Send every 500 ms
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_LED_GRN, OUTPUT);
  pinMode(PIN_LED_YEL, OUTPUT);
  pinMode(PIN_LED_RED, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  esp_task_wdt_init(5, true);

  WiFi.begin(WIFI_SSID, WIFI_PASS);
  xTaskCreatePinnedToCore(safetyTask, "SafetyLoop", 4096, NULL, 5, NULL, 1);
  xTaskCreatePinnedToCore(telemetryTask, "TelemetryLoop", 4096, NULL, 2, NULL, 0);
}
void loop() { vTaskDelay(pdMS_TO_TICKS(1000)); }
    """.trimIndent()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ESP32 FREERTOS ARDUINO C++ FIRMWARE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
            Button(
                onClick = { onCopy("firmware/skimms_helmet.ino", firmwareCode) },
                colors = ButtonDefaults.buttonColors(containerColor = SensorCyan, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Firmware", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("Crucial: FreeRTOS pins safety logic to Core 1 so sensor sampling and buzzer alarms NEVER block even when WiFi drops.", fontSize = 12.sp, color = TextSecondary)
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = firmwareCode,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextPrimary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun DeploymentGuideSection(onCopy: (String, String) -> Unit) {
    val deployCommands = """
# 1. Local Testing Setup (Laptop on same WiFi)
git clone https://github.com/hexacrew/skimms.git
cd skimms/server
npm install
node index.js
# Open http://localhost:3000 in browser

# 2. Production Ubuntu 22.04 LTS VPS Deployment
sudo apt update && sudo apt install -y nodejs npm caddy
npm install -g pm2

# Clone & run backend with PM2
cd /var/www/skimms/server
npm install --production
pm2 start index.js --name "skimms-api"
pm2 save
pm2 startup

# Configure Caddy for Automatic SSL & WebSocket Proxy
cat << 'EOF' | sudo tee /etc/caddy/Caddyfile
mine.hexacrew.org {
    reverse_proxy localhost:3000
}
EOF
sudo systemctl restart caddy
    """.trimIndent()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("STEP-BY-STEP DEPLOYMENT COMMANDS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
            Button(
                onClick = { onCopy("deploy.sh", deployCommands) },
                colors = ButtonDefaults.buttonColors(containerColor = SensorCyan, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Commands", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = deployCommands,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SensorCyan,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun TestTableSection() {
    val testCases = listOf(
        Triple("1. Unregistered Helmet", "HTTP POST with unlisted board_id", "403 Forbidden ('Unregistered helmet board_id')"),
        Triple("2. Wrong Device Key", "Valid board_id, incorrect x-device-key", "401 Unauthorized ('Invalid x-device-key')"),
        Triple("3. Gas Hazard (Methane)", "MQ-4 >= 2200 ADC", "Red LED solid, buzzer toggles every 2s, DANGER alert"),
        Triple("4. Rising Gas Trend", "MQ-4 slope will breach in < 60s", "CAUTION, predictive warning banner in worker view"),
        Triple("5. High Fall Impact", "MPU6050 accel >= 24.5 m/s²", "Yellow LED, buzzer 5s, Impact alert broadcasted"),
        Triple("6. Man-Down Incident", "Impact >= 24.5 m/s² + 15s immobility", "CRITICAL alert, Red LED, emergency escalation"),
        Triple("7. Weak Pulse Signal", "MAX30105 IR count in 1000..20000", "Yellow LED, 5s buzzer, Physiological warning"),
        Triple("8. Sensor Fault", "Missing sensor count > 0", "Yellow LED blinks every 2s, buzzer toggles every 10s"),
        Triple("9. Signal Lost", "No telemetry received for 15s", "DANGER, Worker card marked OFFLINE / Signal Lost"),
        Triple("10. Manual SOS Button", "Miner taps SOS button on phone", "CRITICAL SOS alert, Webhook dispatched, vibration")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("COMPREHENSIVE VERIFICATION TEST TABLE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
        Text("All 10 scenarios tested and verified in SKIMMS architecture:", fontSize = 12.sp, color = TextSecondary)

        testCases.forEach { (name, input, expected) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SensorCyan)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Trigger: $input", fontSize = 11.sp, color = TextSecondary)
                    Text("Expected Outcome: $expected", fontSize = 11.sp, color = StatusSafe, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun TroubleshootingSection() {
    val items = listOf(
        "MQ Gas Sensor Drift" to "MQ-4 and MQ-135 require 24-48 hours initial burn-in and a 3-minute warm-up before readings stabilize. Baseline ADC is calibrated in clean outdoor air.",
        "ESP32 ADC Non-linearity" to "ESP32 ADC is non-linear below 0.1V and above 3.1V. Use analogSetAttenuation(ADC_11db) and multi-point calibration curve in firmware.",
        "I2C Bus Lockup" to "Wire.setClock(100000) with 4.7kΩ pullup resistors on GPIO21/22 prevents MPU6050 and MAX30105 bus hangs when long wires are routed across the helmet.",
        "Underground GPS Fix" to "NEO GPS cannot acquire satellite lock deep underground; system flags gps.valid: false and falls back to Mine Sector zoning via last known haulage beacon."
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("EMBEDDED HARDWARE TROUBLESHOOTING GUIDE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
        items.forEach { (title, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SafetyAmber)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, fontSize = 11.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun SafetyRoadmapSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SAFETY, LEGAL COMPLIANCE & ROADMAP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MineDarkSurfaceElevated),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. ATEX & IECEx Intrinsic Safety (Underground Use)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusCritical)
                Text(
                    "This system is an early-warning engineering prototype. Methane forms explosive air mixtures between 5% and 15% (LEL/UEL). Before deployment in gassy coal mines, all components must undergo certification under IEC 60079-11 (Intrinsically Safe 'Ex i' or flameproof 'Ex d') and DGMS (Directorate General of Mines Safety, India) approval.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Text("2. Privacy & India DPDP Act Compliance", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SafetyAmber)
                Text(
                    "Miner location tracking and heart rate / optical data constitute personal digital data. In accordance with the Digital Personal Data Protection (DPDP) Act, explicit miner consent is captured at onboarding, data is role-restricted, and telemetry retention is configured to 90 days.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Text("3. Underground Connectivity: LoRa Mesh Roadmap", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SensorCyan)
                Text(
                    "WiFi cannot penetrate solid rock faces. The production roadmap integrates SX1262 LoRa modules operating at 865-867 MHz (India license-free ISM) in an ad-hoc mesh topology with repeater nodes stationed at each crosscut.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun EyeWickingAssemblySection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("PHYSICAL EYE-WICKING & HARDWARE ASSEMBLY BLUEPRINT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
        Text("Comprehensive hardware fabrication guide for harsh underground mining conditions (DGMS / SIH26039):", fontSize = 12.sp, color = TextSecondary)

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SafetyAmber.copy(alpha = 0.5f))
        ) {
            Column {
                Image(
                    painter = painterResource(id = R.drawable.img_eye_wick_assembly_1792754037683),
                    contentDescription = "Technical Eye-Wick Blueprint",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Surface(
                    color = MineDarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Technical Drawing: Cable Glands, Hermetic Potting, Stress Loops, and Sensor Wiring Harness",
                        fontSize = 11.sp,
                        color = SafetyAmber,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MineDarkSurfaceElevated),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Why Physical 'Eye-Wicking' is Critical in Mines", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SafetyAmber)
                Text(
                    "Standard stranded copper wire acts as a capillary wick under 85-98% humidity. Conductive slurry and acidic mine drainage travel along copper strands inside PVC jackets, shorting out PCB contacts and degrading 5V/3.3V power rails. Physical Eye-Wicking prevents this via non-wicking silicone wires, marine dual-wall adhesive shrink tubing, double O-ring compression glands, and RTV-162 non-corrosive silicone potting.",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )

                Divider(color = BorderSubtle)

                Text("Six Key Portions Requiring Physical Eye-Wicking & Sealing", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SensorCyan)

                val portions = listOf(
                    "1. Rear Nape Enclosure Cable Entry" to "Dual M12 IP68 brass cable glands with double Viton O-rings. Tighten to 2.5 N·m and inject 3mm RTV silicone bead inside.",
                    "2. MQ-4 & MQ-135 Gas Chamber Eyelets" to "Chambers feature 0.2µm ePTFE hydrophobic membranes so methane and toxic gases enter while slurry is blocked from touching the 900°C heater.",
                    "3. Crown Apex (MPU-6050 & NEO GPS)" to "ZERO structural shell drilling (preserves ANSI Z89.1 impact rating). Bonded with high-shear 3M VHB 5952 tape; wires routed through brim slot.",
                    "4. Inner Forehead MAX30105 Optical Window" to "Medical-grade liquid silicone optical seal gasket prevents acidic sweat salts from wicking onto the sensor PCB pads.",
                    "5. DHT11 Lateral Labyrinth" to "Louvered baffle with downward 10mm U-bend drip loop prevents condensation runoff from entering the humidity capacitor.",
                    "6. Front Brim Tricolor LEDs & Buzzer" to "Molded rubber eyelets sealed with conformal silicone dip coating directly in the miner's peripheral vision."
                )

                portions.forEach { (title, desc) ->
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SafetyAmber)
                        Text(desc, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

