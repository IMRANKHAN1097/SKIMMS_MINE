package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.auth.AuthManager
import com.example.data.repository.SkimmsRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ScreenState {
    HELMET_3D_INSPECTION,
    AUTH,
    ADMIN_CONTROL_ROOM,
    WORKER_PHONE,
    TELEMETRY_SIMULATOR,
    ENGINEERING_DOCS,
    THREEJS_MINE_3D,
    MAPS_RESCUE,
    IMAGE_STUDIO
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check contract wiring before UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SkimmsApp()
                }
            }
        }
    }
}

@Composable
fun SkimmsApp() {
    val context = LocalContext.current
    val repository = remember { SkimmsRepository(context) }
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

    var screenState by remember { mutableStateOf(ScreenState.HELMET_3D_INSPECTION) }
    var previousScreen by remember { mutableStateOf(ScreenState.HELMET_3D_INSPECTION) }

    // Worker session parameters
    var activeWorkerId by remember { mutableStateOf("ESP32_NODE_01") }
    var activeHelmetNumber by remember { mutableStateOf("HLM-042") }
    var activeWorkerName by remember { mutableStateOf("Ramesh Kumar") }

    // Monitor Firebase Auth state
    var firebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val workers by repository.observeWorkers().collectAsState(initial = emptyList())

    // Handle Back Navigation
    BackHandler(enabled = screenState != ScreenState.HELMET_3D_INSPECTION) {
        when (screenState) {
            ScreenState.TELEMETRY_SIMULATOR,
            ScreenState.ENGINEERING_DOCS,
            ScreenState.THREEJS_MINE_3D,
            ScreenState.MAPS_RESCUE,
            ScreenState.IMAGE_STUDIO -> {
                screenState = previousScreen
            }
            ScreenState.ADMIN_CONTROL_ROOM, ScreenState.WORKER_PHONE, ScreenState.AUTH -> {
                screenState = ScreenState.HELMET_3D_INSPECTION
            }
            else -> {
                screenState = ScreenState.HELMET_3D_INSPECTION
            }
        }
    }

    when (screenState) {
        ScreenState.HELMET_3D_INSPECTION -> {
            Helmet3dInspectionScreen(
                repository = repository,
                onNavigateToWorkerPhone = {
                    previousScreen = ScreenState.HELMET_3D_INSPECTION
                    screenState = ScreenState.WORKER_PHONE
                },
                onNavigateToAdmin = {
                    previousScreen = ScreenState.HELMET_3D_INSPECTION
                    screenState = if (firebaseUser != null) ScreenState.ADMIN_CONTROL_ROOM else ScreenState.AUTH
                },
                onNavigateToMine3D = {
                    previousScreen = ScreenState.HELMET_3D_INSPECTION
                    screenState = ScreenState.THREEJS_MINE_3D
                },
                onNavigateToSimulator = {
                    previousScreen = ScreenState.HELMET_3D_INSPECTION
                    screenState = ScreenState.TELEMETRY_SIMULATOR
                },
                onNavigateToRescue = {
                    previousScreen = ScreenState.HELMET_3D_INSPECTION
                    screenState = ScreenState.MAPS_RESCUE
                },
                onNavigateToDocs = {
                    previousScreen = ScreenState.HELMET_3D_INSPECTION
                    screenState = ScreenState.ENGINEERING_DOCS
                }
            )
        }

        ScreenState.AUTH -> {
            AuthScreen(
                repository = repository,
                onAdminLoginSuccess = {
                    screenState = ScreenState.ADMIN_CONTROL_ROOM
                },
                onWorkerLoginSuccess = { workerId, helmetNumber, name ->
                    activeWorkerId = workerId
                    activeHelmetNumber = helmetNumber
                    activeWorkerName = name
                    screenState = ScreenState.WORKER_PHONE
                },
                onBackTo3dHelmet = {
                    screenState = ScreenState.HELMET_3D_INSPECTION
                }
            )
        }

        ScreenState.ADMIN_CONTROL_ROOM -> {
            AdminControlRoomScreen(
                repository = repository,
                onLogout = {
                    AuthManager.signOut(context, credentialManager, coroutineScope) {
                        screenState = ScreenState.AUTH
                    }
                },
                onOpenDocs = {
                    previousScreen = ScreenState.ADMIN_CONTROL_ROOM
                    screenState = ScreenState.ENGINEERING_DOCS
                },
                onOpenSimulator = {
                    previousScreen = ScreenState.ADMIN_CONTROL_ROOM
                    screenState = ScreenState.TELEMETRY_SIMULATOR
                },
                onOpen3dMine = {
                    previousScreen = ScreenState.ADMIN_CONTROL_ROOM
                    screenState = ScreenState.THREEJS_MINE_3D
                },
                onOpenMapsRescue = {
                    previousScreen = ScreenState.ADMIN_CONTROL_ROOM
                    screenState = ScreenState.MAPS_RESCUE
                },
                onOpenImageStudio = {
                    previousScreen = ScreenState.ADMIN_CONTROL_ROOM
                    screenState = ScreenState.IMAGE_STUDIO
                },
                onOpen3dHelmet = {
                    screenState = ScreenState.HELMET_3D_INSPECTION
                }
            )
        }

        ScreenState.WORKER_PHONE -> {
            WorkerPhoneScreen(
                repository = repository,
                workerId = activeWorkerId,
                helmetNumber = activeHelmetNumber,
                workerName = activeWorkerName,
                onSwitchUser = {
                    screenState = ScreenState.AUTH
                },
                onOpenDocs = {
                    previousScreen = ScreenState.WORKER_PHONE
                    screenState = ScreenState.ENGINEERING_DOCS
                },
                onOpenSimulator = {
                    previousScreen = ScreenState.WORKER_PHONE
                    screenState = ScreenState.TELEMETRY_SIMULATOR
                },
                onOpen3dHelmet = {
                    screenState = ScreenState.HELMET_3D_INSPECTION
                }
            )
        }

        ScreenState.TELEMETRY_SIMULATOR -> {
            TelemetrySimulatorBridgeScreen(
                repository = repository,
                onBack = { screenState = previousScreen }
            )
        }

        ScreenState.ENGINEERING_DOCS -> {
            EngineeringDocsScreen(
                onBack = { screenState = previousScreen }
            )
        }

        ScreenState.THREEJS_MINE_3D -> {
            ThreeJsMineScreen(
                workers = workers,
                onBack = { screenState = previousScreen }
            )
        }

        ScreenState.MAPS_RESCUE -> {
            MapsGroundingRescueScreen(
                onBack = { screenState = previousScreen }
            )
        }

        ScreenState.IMAGE_STUDIO -> {
            MineImageStudioScreen(
                onBack = { screenState = previousScreen }
            )
        }
    }
}
