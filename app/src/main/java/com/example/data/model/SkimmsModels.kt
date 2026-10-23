package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONObject

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "worker", // "admin" or "worker"
    val workerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkerRecord(
    @DocumentId val id: String = "",
    val workerId: String = "",
    val helmetNumber: String = "",
    val workerName: String = "",
    val workerPin: String = "",
    val deviceKey: String = "",
    val registeredBy: String = "",
    val status: String = "ONLINE", // "ONLINE", "OFFLINE", "STANDBY"
    val lastSeenMillis: Long = System.currentTimeMillis(),
    val batteryPct: Int = 94,
    val sector: String = "Sector 3 - Stope B",
    val lat: Double = 22.5726,
    val lng: Double = 88.3639,
    val lastRiskScore: Int = 0,
    val lastAlertLevel: String = "SAFE"
)

@IgnoreExtraProperties
data class TelemetryData(
    @DocumentId val id: String = "",
    val workerId: String = "ESP32_NODE_01",
    val helmetNumber: String = "HLM-042",
    val mq4: Double = 380.0,
    val mq135: Double = 261.0,
    val accel: Double = 9.8,
    val ir: Long = 49107L,
    val batteryPct: Int = 92,
    val lat: Double = 22.57,
    val lng: Double = 88.36,
    val gpsValid: Boolean = true,
    val mpuOk: Boolean = true,
    val maxOk: Boolean = true,
    val mq4Ok: Boolean = true,
    val mq135Ok: Boolean = true,
    val healthHazard: Boolean = false,
    val envHazard: Boolean = false,
    val missing: Int = 0,
    val tempC: Double = 28.5,
    val humidityPct: Double = 65.0,
    val dhtOk: Boolean = true,
    val riskScore: Int = 12,
    val alertLevel: String = "SAFE", // SAFE, CAUTION, DANGER, CRITICAL
    val aiReasons: String = "All environmental and physiological parameters nominal.",
    val timestampMillis: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromJson(jsonStr: String, helmetNumber: String = "HLM-01"): TelemetryData {
            val root = JSONObject(jsonStr)
            val boardId = root.optString("board_id", "ESP32_NODE_01")
            val gps = root.optJSONObject("gps")
            val sensors = root.optJSONObject("sensors")
            val status = root.optJSONObject("status")

            return TelemetryData(
                workerId = boardId,
                helmetNumber = helmetNumber,
                lat = gps?.optDouble("lat", 22.57) ?: 22.57,
                lng = gps?.optDouble("lng", 88.36) ?: 88.36,
                gpsValid = gps?.optBoolean("valid", true) ?: true,
                mq4 = sensors?.optDouble("mq4", 380.0) ?: 380.0,
                mq135 = sensors?.optDouble("mq135", 261.0) ?: 261.0,
                accel = sensors?.optDouble("accel", 9.8) ?: 9.8,
                ir = sensors?.optLong("ir", 49107L) ?: 49107L,
                tempC = sensors?.optDouble("temp", 28.5) ?: 28.5,
                humidityPct = sensors?.optDouble("humidity", 65.0) ?: 65.0,
                mpuOk = status?.optBoolean("mpu_ok", true) ?: true,
                maxOk = status?.optBoolean("max_ok", true) ?: true,
                mq4Ok = status?.optBoolean("mq4_ok", true) ?: true,
                mq135Ok = status?.optBoolean("mq135_ok", true) ?: true,
                dhtOk = status?.optBoolean("dht_ok", true) ?: true,
                healthHazard = status?.optBoolean("health_hazard", false) ?: false,
                envHazard = status?.optBoolean("env_hazard", false) ?: false,
                missing = status?.optInt("missing", 0) ?: 0
            )
        }
    }
}

@IgnoreExtraProperties
data class AlertRecord(
    @DocumentId val id: String = "",
    val workerId: String = "",
    val workerName: String = "",
    val helmetNumber: String = "",
    val alertLevel: String = "CAUTION", // CAUTION, DANGER, CRITICAL, SOS
    val reason: String = "",
    val riskScore: Int = 0,
    val acknowledged: Boolean = false,
    val acknowledgedBy: String = "",
    val acknowledgedAtMillis: Long = 0L,
    val escalated: Boolean = false,
    val webhookDispatched: Boolean = false,
    val lat: Double = 22.57,
    val lng: Double = 88.36,
    val createdAtMillis: Long = System.currentTimeMillis()
)
