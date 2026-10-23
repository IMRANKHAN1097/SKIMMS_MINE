package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Real-time sensor telemetry model transmitted by ESP32 helmet nodes.
 * Adheres strictly to Firestore serialization requirements (default zero-arg constructor,
 * nullable server timestamp, document ID annotation).
 */
@IgnoreExtraProperties
data class HelmetTelemetry(
    @DocumentId val id: String = "",
    val workerId: String = "",
    val helmetNumber: String = "",
    val mq4: Double = 0.0,
    val mq135: Double = 0.0,
    val accel: Double = 9.8,
    val ir: Long = 0L,
    val batteryPct: Int = 100,
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
    val riskScore: Int = 0,
    val alertLevel: String = "SAFE", // SAFE, CAUTION, DANGER, CRITICAL
    val aiReasons: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
