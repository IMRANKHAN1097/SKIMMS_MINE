package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AlertRecord
import com.example.data.model.TelemetryData
import com.example.data.model.WorkerRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

class SkimmsRepository(private val db: FirebaseFirestore) {
    companion object {
        private const val TAG = "SkimmsRepository"

        private val cachedWorkers = ConcurrentHashMap<String, WorkerRecord>()
        private val cachedAlerts = mutableListOf<AlertRecord>()
        private val cachedTelemetry = ConcurrentHashMap<String, MutableList<TelemetryData>>()

        init {
            val seedWorkers = listOf(
                WorkerRecord(
                    id = "ESP32_NODE_01",
                    workerId = "ESP32_NODE_01",
                    helmetNumber = "HLM-042",
                    workerName = "Ramesh Kumar",
                    workerPin = "1234",
                    deviceKey = "kavach_a1b2c3d4e5f607182930",
                    registeredBy = "admin_seed",
                    status = "ONLINE",
                    lastSeenMillis = System.currentTimeMillis(),
                    batteryPct = 94,
                    sector = "Sector 3 - Stope B (Active Face)",
                    lat = 22.5735,
                    lng = 88.3645,
                    lastRiskScore = 15,
                    lastAlertLevel = "SAFE"
                ),
                WorkerRecord(
                    id = "ESP32_NODE_02",
                    workerId = "ESP32_NODE_02",
                    helmetNumber = "HLM-019",
                    workerName = "Subhash Bose",
                    workerPin = "5678",
                    deviceKey = "kavach_84930219485720194857",
                    registeredBy = "admin_seed",
                    status = "ONLINE",
                    lastSeenMillis = System.currentTimeMillis() - 4000,
                    batteryPct = 88,
                    sector = "Sector 1 - Haulage Incline",
                    lat = 22.5718,
                    lng = 88.3628,
                    lastRiskScore = 22,
                    lastAlertLevel = "SAFE"
                ),
                WorkerRecord(
                    id = "ESP32_NODE_03",
                    workerId = "ESP32_NODE_03",
                    helmetNumber = "HLM-033",
                    workerName = "Priya Das",
                    workerPin = "9999",
                    deviceKey = "kavach_38472910485729104829",
                    registeredBy = "admin_seed",
                    status = "STANDBY",
                    lastSeenMillis = System.currentTimeMillis() - 12000,
                    batteryPct = 78,
                    sector = "Sector 4 - Return Airway Shaft",
                    lat = 22.5742,
                    lng = 88.3658,
                    lastRiskScore = 45,
                    lastAlertLevel = "CAUTION"
                )
            )
            seedWorkers.forEach { cachedWorkers[it.workerId] = it }

            cachedAlerts.add(
                AlertRecord(
                    id = "alt_seed_01",
                    workerId = "ESP32_NODE_03",
                    workerName = "Priya Das",
                    helmetNumber = "HLM-033",
                    alertLevel = "CAUTION",
                    reason = "Ventilation fluctuation detected in Sector 4 Return Airway.",
                    riskScore = 45,
                    acknowledged = false,
                    escalated = false,
                    lat = 22.5742,
                    lng = 88.3658,
                    createdAtMillis = System.currentTimeMillis() - 150000
                )
            )
        }
    }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun currentUserId(): String? = auth.currentUser?.uid
    fun currentUserEmail(): String? = auth.currentUser?.email
    fun currentUserName(): String? = auth.currentUser?.displayName

    // --- WORKERS ---
    fun observeWorkers(): Flow<List<WorkerRecord>> {
        // Invariant 9: Auth-gate collection queries to prevent CancellationException when unauthenticated
        if (auth.currentUser == null) {
            return flowOf(cachedWorkers.values.toList())
        }
        return db.collection("workers")
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(WorkerRecord::class.java)
                list.forEach { cachedWorkers[it.workerId] = it }
                if (list.isEmpty()) cachedWorkers.values.toList() else list
            }
            .catch { e ->
                Log.w(TAG, "Falling back to cached workers: ${e.message}")
                emit(cachedWorkers.values.toList())
            }
    }

    suspend fun registerWorker(
        workerId: String,
        helmetNumber: String,
        workerName: String,
        workerPin: String,
        sector: String = "Sector 2 - Main Drift"
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: "sys_admin"
        val randomBytes = ByteArray(16)
        SecureRandom().nextBytes(randomBytes)
        val deviceKey = "kavach_" + randomBytes.joinToString("") { "%02x".format(it) }

        val worker = WorkerRecord(
            id = workerId,
            workerId = workerId,
            helmetNumber = helmetNumber,
            workerName = workerName,
            workerPin = workerPin,
            deviceKey = deviceKey,
            registeredBy = uid,
            status = "ONLINE",
            lastSeenMillis = System.currentTimeMillis(),
            batteryPct = 96,
            sector = sector,
            lat = 22.5726 + (Math.random() - 0.5) * 0.005,
            lng = 88.3639 + (Math.random() - 0.5) * 0.005,
            lastRiskScore = 10,
            lastAlertLevel = "SAFE"
        )

        cachedWorkers[workerId] = worker

        if (auth.currentUser != null) {
            try {
                db.collection("workers").document(workerId).set(worker).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error registering worker in Firestore", e)
            }
        }
        return Result.success(deviceKey)
    }

    suspend fun removeWorker(workerId: String): Result<Unit> {
        cachedWorkers.remove(workerId)
        if (auth.currentUser != null) {
            try {
                db.collection("workers").document(workerId).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Error removing worker from Firestore", e)
            }
        }
        return Result.success(Unit)
    }

    suspend fun getWorker(workerId: String): WorkerRecord? {
        if (auth.currentUser == null) {
            return cachedWorkers[workerId]
        }
        return try {
            val doc = db.collection("workers").document(workerId).get().await()
            doc.toObject(WorkerRecord::class.java) ?: cachedWorkers[workerId]
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to cached worker $workerId: ${e.message}")
            cachedWorkers[workerId]
        }
    }

    // --- ALERTS ---
    fun observeAlerts(): Flow<List<AlertRecord>> {
        if (auth.currentUser == null) {
            return flowOf(cachedAlerts.toList())
        }
        return db.collection("alerts")
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(AlertRecord::class.java)
                if (list.isNotEmpty()) {
                    synchronized(cachedAlerts) {
                        cachedAlerts.clear()
                        cachedAlerts.addAll(list)
                    }
                    list
                } else {
                    cachedAlerts.toList()
                }
            }
            .catch { e ->
                Log.w(TAG, "Falling back to cached alerts: ${e.message}")
                emit(cachedAlerts.toList())
            }
    }

    suspend fun recordAlert(alert: AlertRecord): Result<Unit> {
        val docId = if (alert.id.isNotBlank()) alert.id else "alt_${System.currentTimeMillis()}"
        val finalAlert = alert.copy(id = docId)

        synchronized(cachedAlerts) {
            cachedAlerts.add(0, finalAlert)
            if (cachedAlerts.size > 100) cachedAlerts.removeAt(cachedAlerts.size - 1)
        }

        // Update local worker state
        cachedWorkers[alert.workerId]?.let { w ->
            cachedWorkers[alert.workerId] = w.copy(
                lastAlertLevel = alert.alertLevel,
                lastRiskScore = alert.riskScore,
                lastSeenMillis = System.currentTimeMillis()
            )
        }

        if (auth.currentUser != null) {
            try {
                db.collection("alerts").document(docId).set(finalAlert).await()
                db.collection("workers").document(alert.workerId)
                    .update(
                        mapOf(
                            "lastAlertLevel" to alert.alertLevel,
                            "lastRiskScore" to alert.riskScore,
                            "lastSeenMillis" to System.currentTimeMillis()
                        )
                    ).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error recording alert in Firestore", e)
            }
        }
        return Result.success(Unit)
    }

    suspend fun acknowledgeAlert(alertId: String, adminName: String): Result<Unit> {
        synchronized(cachedAlerts) {
            val idx = cachedAlerts.indexOfFirst { it.id == alertId }
            if (idx != -1) {
                cachedAlerts[idx] = cachedAlerts[idx].copy(
                    acknowledged = true,
                    acknowledgedBy = adminName,
                    acknowledgedAtMillis = System.currentTimeMillis()
                )
            }
        }

        if (auth.currentUser != null) {
            try {
                db.collection("alerts").document(alertId)
                    .update(
                        mapOf(
                            "acknowledged" to true,
                            "acknowledgedBy" to adminName,
                            "acknowledgedAtMillis" to System.currentTimeMillis()
                        )
                    ).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error acknowledging alert in Firestore", e)
            }
        }
        return Result.success(Unit)
    }

    // --- TELEMETRY ---
    suspend fun submitTelemetry(telemetry: TelemetryData): Result<Unit> {
        val docId = "tel_${telemetry.workerId}_${System.currentTimeMillis()}"
        val payload = telemetry.copy(id = docId)

        cachedTelemetry.getOrPut(telemetry.workerId) { mutableListOf() }.add(0, payload)

        cachedWorkers[telemetry.workerId]?.let { w ->
            cachedWorkers[telemetry.workerId] = w.copy(
                lastSeenMillis = System.currentTimeMillis(),
                batteryPct = telemetry.batteryPct,
                lat = telemetry.lat,
                lng = telemetry.lng,
                lastRiskScore = telemetry.riskScore,
                lastAlertLevel = telemetry.alertLevel,
                status = "ONLINE"
            )
        }

        if (auth.currentUser != null) {
            try {
                db.collection("telemetry").document(docId).set(payload).await()
                db.collection("workers").document(telemetry.workerId)
                    .update(
                        mapOf(
                            "lastSeenMillis" to System.currentTimeMillis(),
                            "batteryPct" to telemetry.batteryPct,
                            "lat" to telemetry.lat,
                            "lng" to telemetry.lng,
                            "lastRiskScore" to telemetry.riskScore,
                            "lastAlertLevel" to telemetry.alertLevel,
                            "status" to "ONLINE"
                        )
                    ).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting telemetry to Firestore", e)
            }
        }
        return Result.success(Unit)
    }

    fun observeTelemetryForWorker(workerId: String): Flow<List<TelemetryData>> {
        if (auth.currentUser == null) {
            return flowOf(cachedTelemetry[workerId] ?: emptyList())
        }
        return db.collection("telemetry")
            .whereEqualTo("workerId", workerId)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(TelemetryData::class.java)
                if (list.isNotEmpty()) {
                    cachedTelemetry[workerId] = list.toMutableList()
                    list
                } else {
                    cachedTelemetry[workerId] ?: emptyList()
                }
            }
            .catch { e ->
                Log.w(TAG, "Fallback to cached telemetry for $workerId: ${e.message}")
                emit(cachedTelemetry[workerId] ?: emptyList())
            }
    }

    fun observeActiveTelemetry(workerId: String = "ESP32_NODE_01"): Flow<TelemetryData> {
        return observeTelemetryForWorker(workerId).map { list ->
            list.firstOrNull() ?: cachedTelemetry[workerId]?.firstOrNull() ?: TelemetryData(
                workerId = workerId,
                mq4 = 380.0,
                mq135 = 261.0,
                accel = 9.8,
                ir = 49107L,
                tempC = 28.5,
                humidityPct = 65.0
            )
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        if (auth.currentUser == null) return
        try {
            val snapshot = db.collection("workers").limit(1).get().await()
            if (snapshot.isEmpty) {
                for (worker in cachedWorkers.values) {
                    db.collection("workers").document(worker.workerId).set(worker).await()
                }
                for (alert in cachedAlerts) {
                    db.collection("alerts").document(alert.id).set(alert).await()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Seed data check skipped: ${e.message}")
        }
    }
}
