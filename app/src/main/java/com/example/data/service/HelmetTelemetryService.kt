package com.example.data.service

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.HelmetTelemetry
import com.example.data.model.WorkerProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
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

/**
 * Service class handling complete Firestore CRUD operations for:
 * 1. Worker Profiles ('workers' collection)
 * 2. Real-time Sensor Telemetry ('telemetry' collection)
 */
class HelmetTelemetryService(private val db: FirebaseFirestore) {

    companion object {
        private const val TAG = "HelmetTelemetryService"
        const val WORKERS_COLLECTION = "workers"
        const val TELEMETRY_COLLECTION = "telemetry"

        // Local cache fallback when unauthenticated / offline
        private val cachedWorkers = ConcurrentHashMap<String, WorkerProfile>()
        private val cachedTelemetry = ConcurrentHashMap<String, MutableList<HelmetTelemetry>>()

        init {
            // Seed initial local fallback data
            val defaultWorkers = listOf(
                WorkerProfile(
                    id = "ESP32_NODE_01",
                    workerId = "ESP32_NODE_01",
                    helmetNumber = "HLM-042",
                    workerName = "Ramesh Kumar",
                    workerPin = "1234",
                    deviceKey = "kavach_a1b2c3d4e5f607182930",
                    status = "ONLINE",
                    batteryPct = 94,
                    sector = "Sector 3 - Stope B",
                    lat = 22.5735,
                    lng = 88.3645,
                    lastRiskScore = 12,
                    lastAlertLevel = "SAFE"
                ),
                WorkerProfile(
                    id = "ESP32_NODE_02",
                    workerId = "ESP32_NODE_02",
                    helmetNumber = "HLM-019",
                    workerName = "Subhash Bose",
                    workerPin = "5678",
                    deviceKey = "kavach_84930219485720194857",
                    status = "ONLINE",
                    batteryPct = 88,
                    sector = "Sector 1 - Haulage Incline",
                    lat = 22.5718,
                    lng = 88.3628,
                    lastRiskScore = 20,
                    lastAlertLevel = "SAFE"
                ),
                WorkerProfile(
                    id = "ESP32_NODE_03",
                    workerId = "ESP32_NODE_03",
                    helmetNumber = "HLM-033",
                    workerName = "Priya Das",
                    workerPin = "9999",
                    deviceKey = "kavach_38472910485729104829",
                    status = "STANDBY",
                    batteryPct = 76,
                    sector = "Sector 4 - Return Airway Shaft",
                    lat = 22.5742,
                    lng = 88.3658,
                    lastRiskScore = 45,
                    lastAlertLevel = "CAUTION"
                )
            )
            defaultWorkers.forEach { cachedWorkers[it.workerId] = it }
        }
    }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    // --- WORKER PROFILES CRUD ---

    /**
     * CREATE: Stores a new worker profile and generates their secure device key.
     */
    suspend fun createWorkerProfile(
        workerId: String,
        helmetNumber: String,
        workerName: String,
        workerPin: String,
        sector: String = "Sector 2 - Main Drift"
    ): Result<String> {
        return try {
            val randomBytes = ByteArray(16)
            SecureRandom().nextBytes(randomBytes)
            val deviceKey = "kavach_" + randomBytes.joinToString("") { "%02x".format(it) }

            val profile = WorkerProfile(
                id = workerId,
                workerId = workerId,
                helmetNumber = helmetNumber,
                workerName = workerName,
                workerPin = workerPin,
                deviceKey = deviceKey,
                sector = sector,
                status = "ONLINE",
                batteryPct = 100,
                lat = 22.5726 + (Math.random() - 0.5) * 0.005,
                lng = 88.3639 + (Math.random() - 0.5) * 0.005,
                lastRiskScore = 0,
                lastAlertLevel = "SAFE"
            )

            // Cache locally
            cachedWorkers[workerId] = profile

            if (auth.currentUser != null) {
                db.collection(WORKERS_COLLECTION).document(workerId).set(profile).await()
            }
            Result.success(deviceKey)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating worker profile for $workerId", e)
            Result.failure(e)
        }
    }

    /**
     * READ: Fetches a single worker profile by worker ID.
     */
    suspend fun getWorkerProfile(workerId: String): WorkerProfile? {
        if (auth.currentUser == null) {
            return cachedWorkers[workerId]
        }
        return try {
            val doc = db.collection(WORKERS_COLLECTION).document(workerId).get().await()
            doc.toObject(WorkerProfile::class.java) ?: cachedWorkers[workerId]
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching worker profile from Firestore; falling back to local cache: ${e.message}")
            cachedWorkers[workerId]
        }
    }

    /**
     * READ: Real-time observation flow of all active worker profiles.
     * Auth-gated: Returns cached list when unauthenticated to prevent CancellationException.
     */
    fun observeWorkerProfiles(): Flow<List<WorkerProfile>> {
        if (auth.currentUser == null) {
            return flowOf(cachedWorkers.values.toList())
        }
        return db.collection(WORKERS_COLLECTION)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(WorkerProfile::class.java)
                list.forEach { cachedWorkers[it.workerId] = it }
                if (list.isEmpty()) cachedWorkers.values.toList() else list
            }
            .catch { e ->
                Log.w(TAG, "observeWorkerProfiles Firestore stream interrupted; falling back to local cache: ${e.message}")
                emit(cachedWorkers.values.toList())
            }
    }

    /**
     * UPDATE: Updates specific fields on an existing worker profile.
     */
    suspend fun updateWorkerProfile(workerId: String, updates: Map<String, Any?>): Result<Unit> {
        val existing = cachedWorkers[workerId]
        if (existing != null) {
            val updated = existing.copy(
                status = (updates["status"] as? String) ?: existing.status,
                batteryPct = (updates["batteryPct"] as? Int) ?: existing.batteryPct,
                lastRiskScore = (updates["lastRiskScore"] as? Int) ?: existing.lastRiskScore,
                lastAlertLevel = (updates["lastAlertLevel"] as? String) ?: existing.lastAlertLevel,
                sector = (updates["sector"] as? String) ?: existing.sector
            )
            cachedWorkers[workerId] = updated
        }

        if (auth.currentUser == null) {
            return Result.success(Unit)
        }

        return try {
            db.collection(WORKERS_COLLECTION).document(workerId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating worker profile $workerId", e)
            Result.failure(e)
        }
    }

    /**
     * DELETE: Decommissions / removes a worker profile from Firestore.
     */
    suspend fun deleteWorkerProfile(workerId: String): Result<Unit> {
        cachedWorkers.remove(workerId)
        if (auth.currentUser == null) {
            return Result.success(Unit)
        }
        return try {
            db.collection(WORKERS_COLLECTION).document(workerId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting worker profile $workerId", e)
            Result.failure(e)
        }
    }

    // --- REAL-TIME SENSOR DATA (HELMET TELEMETRY) CRUD ---

    /**
     * CREATE: Ingests and stores a new HelmetTelemetry sensor packet.
     */
    suspend fun recordTelemetry(telemetry: HelmetTelemetry): Result<String> {
        val docId = if (telemetry.id.isNotBlank()) telemetry.id else "tel_${telemetry.workerId}_${System.currentTimeMillis()}"
        val finalRecord = telemetry.copy(id = docId)

        // Store in local cache
        cachedTelemetry.getOrPut(telemetry.workerId) { mutableListOf() }.add(0, finalRecord)

        // Also update worker profile status
        val workerUpdates = mapOf<String, Any?>(
            "status" to "ONLINE",
            "batteryPct" to telemetry.batteryPct,
            "lastRiskScore" to telemetry.riskScore,
            "lastAlertLevel" to telemetry.alertLevel,
            "lat" to telemetry.lat,
            "lng" to telemetry.lng,
            "lastSeen" to FieldValue.serverTimestamp()
        )
        updateWorkerProfile(telemetry.workerId, workerUpdates)

        if (auth.currentUser == null) {
            return Result.success(docId)
        }

        return try {
            db.collection(TELEMETRY_COLLECTION).document(docId).set(finalRecord).await()
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving telemetry to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * READ: Fetches a single HelmetTelemetry record by document ID.
     */
    suspend fun getTelemetry(telemetryId: String): HelmetTelemetry? {
        if (auth.currentUser == null) {
            return cachedTelemetry.values.flatten().find { it.id == telemetryId }
        }
        return try {
            val doc = db.collection(TELEMETRY_COLLECTION).document(telemetryId).get().await()
            doc.toObject(HelmetTelemetry::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Error getting telemetry $telemetryId: ${e.message}")
            cachedTelemetry.values.flatten().find { it.id == telemetryId }
        }
    }

    /**
     * READ: Retrieves the most recent telemetry packet for a specific worker.
     */
    suspend fun getLatestTelemetryForWorker(workerId: String): HelmetTelemetry? {
        if (auth.currentUser == null) {
            return cachedTelemetry[workerId]?.firstOrNull()
        }
        return try {
            val snapshot = db.collection(TELEMETRY_COLLECTION)
                .whereEqualTo("workerId", workerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            snapshot.toObjects(HelmetTelemetry::class.java).firstOrNull()
                ?: cachedTelemetry[workerId]?.firstOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching latest telemetry for $workerId: ${e.message}")
            cachedTelemetry[workerId]?.firstOrNull()
        }
    }

    /**
     * READ: Real-time observation flow of sensor telemetry for a helmet node.
     * Auth-gated: Safely yields cached telemetry when unauthenticated.
     */
    fun observeRealtimeTelemetry(workerId: String): Flow<List<HelmetTelemetry>> {
        if (auth.currentUser == null) {
            return flowOf(cachedTelemetry[workerId] ?: emptyList())
        }
        return db.collection(TELEMETRY_COLLECTION)
            .whereEqualTo("workerId", workerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(HelmetTelemetry::class.java)
                if (list.isNotEmpty()) {
                    cachedTelemetry[workerId] = list.toMutableList()
                    list
                } else {
                    cachedTelemetry[workerId] ?: emptyList()
                }
            }
            .catch { e ->
                Log.w(TAG, "observeRealtimeTelemetry Firestore stream interrupted for $workerId: ${e.message}")
                emit(cachedTelemetry[workerId] ?: emptyList())
            }
    }

    /**
     * DELETE: Removes an individual telemetry record from Firestore.
     */
    suspend fun deleteTelemetry(telemetryId: String): Result<Unit> {
        cachedTelemetry.values.forEach { list -> list.removeAll { it.id == telemetryId } }
        if (auth.currentUser == null) {
            return Result.success(Unit)
        }
        return try {
            db.collection(TELEMETRY_COLLECTION).document(telemetryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting telemetry record $telemetryId", e)
            Result.failure(e)
        }
    }
}
