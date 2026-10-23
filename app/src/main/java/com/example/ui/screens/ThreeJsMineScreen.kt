package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.WorkerRecord
import com.example.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeJsMineScreen(
    workers: List<WorkerRecord>,
    onBack: () -> Unit
) {
    var selectedSector by remember { mutableStateOf("ALL") }
    var showMethanePlume by remember { mutableStateOf(true) }
    var showVentilation by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val threeJsHtml = remember {
        generateThreeJsHtml()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("3D Subterranean Mine Twin", fontWeight = FontWeight.Black, fontSize = 16.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = SafetyAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("Three.js WebGL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafetyAmber, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text("Interactive Stope Geometry • Smart Helmet 3D Spatial Telemetry", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("threejs_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("resetCamera()", null)
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Camera View", tint = SensorCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MineDarkSurface)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Full-bleed WebGL 3D Canvas in stable WebView container
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
                        loadDataWithBaseURL("https://threejs.org/", threeJsHtml, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Compact Collapsible Floating Control Dock (<= 15% viewport footprint)
            Surface(
                color = MineDarkSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(BorderSubtle, SafetyAmber.copy(alpha = 0.4f)))),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3D SPATIAL TELEMETRY CONTROLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = showMethanePlume,
                                onClick = {
                                    showMethanePlume = !showMethanePlume
                                    webViewRef?.evaluateJavascript("toggleMethane(${showMethanePlume})", null)
                                },
                                label = { Text("Gas Plume", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = showVentilation,
                                onClick = {
                                    showVentilation = !showVentilation
                                    webViewRef?.evaluateJavascript("toggleVentilation(${showVentilation})", null)
                                },
                                label = { Text("Airflow", fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedSector = "Sector 3"
                                webViewRef?.evaluateJavascript("focusNode('ESP32_NODE_01')", null)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MineDarkSurfaceElevated, contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Focus Node 1 (Ramesh)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                selectedSector = "Sector 1"
                                webViewRef?.evaluateJavascript("focusNode('ESP32_NODE_02')", null)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MineDarkSurfaceElevated, contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Focus Node 2 (Subhash)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                selectedSector = "Sector 4"
                                webViewRef?.evaluateJavascript("focusNode('ESP32_NODE_03')", null)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MineDarkSurfaceElevated, contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Focus Node 3 (Priya)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun generateThreeJsHtml(): String {
    return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <style>
    body { margin: 0; overflow: hidden; background: #05080f; font-family: sans-serif; touch-action: none; }
    #canvas-container { width: 100vw; height: 100vh; position: absolute; left: 0; top: 0; }
    #hud { position: absolute; top: 12px; left: 12px; color: #00e5ff; font-size: 11px; pointer-events: none; text-shadow: 0 1px 3px rgba(0,0,0,0.8); }
  </style>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js"></script>
</head>
<body>
  <div id="canvas-container"></div>
  <div id="hud">SKIMMS 3D DIGITAL TWIN • -180m LEVEL DRIFT • 60 FPS PBR</div>

  <script>
    const container = document.getElementById('canvas-container');
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x060913);
    scene.fog = new THREE.FogExp2(0x060913, 0.025);

    const camera = new THREE.PerspectiveCamera(50, window.innerWidth / window.innerHeight, 0.1, 150);
    camera.position.set(0, 14, 26);

    const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.1;
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    container.appendChild(renderer.domElement);

    const controls = new THREE.OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.05;
    controls.maxPolarAngle = Math.PI / 2 - 0.05;
    controls.target.set(0, 2, 0);

    // Studio PBR Lighting
    const keyLight = new THREE.DirectionalLight(0xffecd2, 1.8);
    keyLight.position.set(12, 18, 14);
    keyLight.castShadow = true;
    keyLight.shadow.mapSize.width = 1024;
    keyLight.shadow.mapSize.height = 1024;
    scene.add(keyLight);

    const hemiLight = new THREE.HemisphereLight(0x00e5ff, 0x1a1200, 0.6);
    scene.add(hemiLight);

    // Procedural Rock Canvas Bump Map
    function createProceduralNoiseTexture() {
      const cv = document.createElement('canvas');
      cv.width = 256; cv.height = 256;
      const ctx = cv.getContext('2d');
      ctx.fillStyle = '#222'; ctx.fillRect(0, 0, 256, 256);
      for(let i=0; i<3000; i++) {
        const x = Math.random()*256, y = Math.random()*256, r = Math.random()*4;
        ctx.fillStyle = Math.random() > 0.5 ? '#444' : '#111';
        ctx.beginPath(); ctx.arc(x,y,r,0,Math.PI*2); ctx.fill();
      }
      return new THREE.CanvasTexture(cv);
    }
    const noiseTex = createProceduralNoiseTexture();
    noiseTex.wrapS = noiseTex.wrapT = THREE.RepeatWrapping;
    noiseTex.repeat.set(4, 4);

    // Subterranean Mine Drift Floor
    const floorGeo = new THREE.PlaneGeometry(60, 60);
    const floorMat = new THREE.MeshStandardMaterial({
      color: 0x141a24,
      roughness: 0.85,
      metalness: 0.1,
      bumpMap: noiseTex,
      bumpScale: 0.08
    });
    const floor = new THREE.Mesh(floorGeo, floorMat);
    floor.rotation.x = -Math.PI / 2;
    floor.receiveShadow = true;
    scene.add(floor);

    // Composite Procedural Model: Mine Tunnel Arches (Timber + Steel + Haulage Rails)
    const tunnelGroup = new THREE.Group();
    const beamMat = new THREE.MeshStandardMaterial({ color: 0x4a3b32, roughness: 0.7, metalness: 0.2 });
    const steelMat = new THREE.MeshStandardMaterial({ color: 0x718290, roughness: 0.3, metalness: 0.8 });

    for (let z = -20; z <= 20; z += 4) {
      const arch = new THREE.Group();
      // Left upright beam
      const postL = new THREE.Mesh(new THREE.BoxGeometry(0.3, 4.5, 0.3), beamMat);
      postL.position.set(-4, 2.25, 0); postL.castShadow = true; arch.add(postL);
      // Right upright beam
      const postR = new THREE.Mesh(new THREE.BoxGeometry(0.3, 4.5, 0.3), beamMat);
      postR.position.set(4, 2.25, 0); postR.castShadow = true; arch.add(postR);
      // Top header beam
      const header = new THREE.Mesh(new THREE.BoxGeometry(8.3, 0.35, 0.35), beamMat);
      header.position.set(0, 4.5, 0); header.castShadow = true; arch.add(header);
      // Cross brace corner accents
      const gusset = new THREE.Mesh(new THREE.BoxGeometry(0.8, 0.15, 0.25), steelMat);
      gusset.position.set(-3.5, 4.2, 0); gusset.rotation.z = Math.PI / 4; arch.add(gusset);
      arch.position.z = z;
      tunnelGroup.add(arch);
    }
    // Haulage Railway Tracks
    const railMat = new THREE.MeshStandardMaterial({ color: 0x8898a8, roughness: 0.25, metalness: 0.9 });
    const railL = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.15, 44), railMat);
    railL.position.set(-0.8, 0.08, 0); tunnelGroup.add(railL);
    const railR = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.15, 44), railMat);
    railR.position.set(0.8, 0.08, 0); tunnelGroup.add(railR);
    scene.add(tunnelGroup);

    // Multi-Part Composite 3D Smart Helmet Model Factory (3-12 parts)
    const nodeObjects = {};

    function createSmartHelmetNode(id, label, colorHex, x, z) {
      const helmetGroup = new THREE.Group();

      // 1. Contact shadow disc
      const shadowGeo = new THREE.CircleGeometry(0.8, 16);
      const shadowMat = new THREE.MeshBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.5 });
      const shadow = new THREE.Mesh(shadowGeo, shadowMat);
      shadow.rotation.x = -Math.PI / 2; shadow.position.y = 0.01;
      helmetGroup.add(shadow);

      // 2. Base Pedestal / Miner figure base
      const baseMesh = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.45, 0.4, 16),
        new THREE.MeshStandardMaterial({ color: 0x222a36, roughness: 0.6 }));
      baseMesh.position.y = 0.2; baseMesh.castShadow = true;
      helmetGroup.add(baseMesh);

      // 3. Helmet Outer Shell (Polycarbonate safety dome)
      const domeGeo = new THREE.SphereGeometry(0.65, 16, 12, 0, Math.PI * 2, 0, Math.PI * 0.55);
      const domeMat = new THREE.MeshStandardMaterial({ color: colorHex, roughness: 0.35, metalness: 0.3 });
      const dome = new THREE.Mesh(domeGeo, domeMat);
      dome.position.y = 0.7; dome.castShadow = true;
      helmetGroup.add(dome);

      // 4. Helmet Brim Visor
      const brimGeo = new THREE.CylinderGeometry(0.72, 0.72, 0.06, 16);
      const brimMat = new THREE.MeshStandardMaterial({ color: 0x111620, roughness: 0.4, metalness: 0.6 });
      const brim = new THREE.Mesh(brimGeo, brimMat);
      brim.position.y = 0.68;
      helmetGroup.add(brim);

      // 5. IoT Sensor Visor / Headlamp crest (Glowing LED)
      const lampGeo = new THREE.BoxGeometry(0.3, 0.2, 0.2);
      const lampMat = new THREE.MeshBasicMaterial({ color: colorHex });
      const lamp = new THREE.Mesh(lampGeo, lampMat);
      lamp.position.set(0, 0.85, 0.6);
      helmetGroup.add(lamp);

      // 6. Point Light emitting from helmet headlamp
      const light = new THREE.PointLight(colorHex, 1.2, 8);
      light.position.set(0, 1.0, 0.8);
      helmetGroup.add(light);

      // 7. LoRa / WiFi Antenna Stalk
      const antGeo = new THREE.CylinderGeometry(0.02, 0.02, 0.4, 8);
      const antMat = new THREE.MeshStandardMaterial({ color: 0x111111, metalness: 0.9, roughness: 0.1 });
      const ant = new THREE.Mesh(antGeo, antMat);
      ant.position.set(0.35, 1.25, -0.2);
      ant.rotation.z = -0.15;
      helmetGroup.add(ant);

      // 8. Kavach Protective Shield Emblem
      const badgeGeo = new THREE.BoxGeometry(0.18, 0.18, 0.04);
      const badgeMat = new THREE.MeshStandardMaterial({ color: 0xffb300, metalness: 0.8, roughness: 0.2 });
      const badge = new THREE.Mesh(badgeGeo, badgeMat);
      badge.position.set(0, 0.95, -0.6);
      helmetGroup.add(badge);

      helmetGroup.position.set(x, 0, z);
      scene.add(helmetGroup);
      nodeObjects[id] = helmetGroup;
    }

    // Spawn miner helmet nodes
    createSmartHelmetNode('ESP32_NODE_01', 'Ramesh (Node 1)', 0x00e676, -1.8, 4.0);
    createSmartHelmetNode('ESP32_NODE_02', 'Subhash (Node 2)', 0x00e5ff, 1.5, -6.0);
    createSmartHelmetNode('ESP32_NODE_03', 'Priya (Node 3)', 0xffc107, 0.2, 12.0);

    // Methane Gas Plume Hazard Simulation (Translucent Particles)
    const gasGroup = new THREE.Group();
    const gasGeo = new THREE.DodecahedronGeometry(1.2, 1);
    const gasMat = new THREE.MeshBasicMaterial({ color: 0xff3b30, transparent: true, opacity: 0.22, wireframe: true });
    for (let i = 0; i < 6; i++) {
      const g = new THREE.Mesh(gasGeo, gasMat);
      g.position.set(-2.0 + Math.random(), 2.0 + Math.random() * 2, 8.0 + Math.random() * 3);
      gasGroup.add(g);
    }
    scene.add(gasGroup);

    // Airflow Streamer Particles
    const airGroup = new THREE.Group();
    const airMat = new THREE.MeshBasicMaterial({ color: 0x00e5ff, transparent: true, opacity: 0.4 });
    for (let i = 0; i < 30; i++) {
      const p = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.08, 0.8), airMat);
      p.position.set((Math.random() - 0.5) * 6, 1 + Math.random() * 2.5, (Math.random() - 0.5) * 40);
      airGroup.add(p);
    }
    scene.add(airGroup);

    window.toggleMethane = (show) => { gasGroup.visible = show; };
    window.toggleVentilation = (show) => { airGroup.visible = show; };
    window.resetCamera = () => {
      camera.position.set(0, 14, 26);
      controls.target.set(0, 2, 0);
    };
    window.focusNode = (id) => {
      const target = nodeObjects[id];
      if (target) {
        controls.target.copy(target.position);
        camera.position.set(target.position.x + 3, target.position.y + 4, target.position.z + 5);
      }
    };

    // Lossless 60 FPS animation loop with exponential camera damping
    let prevTime = performance.now();
    function animate() {
      requestAnimationFrame(animate);
      const now = performance.now();
      const dt = (now - prevTime) * 0.001;
      prevTime = now;

      // Animate airflow stream
      airGroup.children.forEach(p => {
        p.position.z -= 6.0 * dt;
        if (p.position.z < -20) p.position.z = 20;
      });

      // Gently breathe gas plume
      gasGroup.rotation.y += 0.2 * dt;

      controls.update();
      renderer.render(scene, camera);
    }
    animate();

    window.addEventListener('resize', () => {
      camera.aspect = window.innerWidth / window.innerHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(window.innerWidth, window.innerHeight);
    });
  </script>
</body>
</html>
    """.trimIndent()
}
