package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Worker profile model stored in Firestore.
 */
@IgnoreExtraProperties
data class WorkerProfile(
    @DocumentId val id: String = "",
    val workerId: String = "",
    val helmetNumber: String = "",
    val workerName: String = "",
    val workerPin: String = "",
    val deviceKey: String = "",
    val sector: String = "Sector 2 - Main Drift",
    val status: String = "ONLINE", // ONLINE, OFFLINE, STANDBY
    val batteryPct: Int = 100,
    val lat: Double = 22.57,
    val lng: Double = 88.36,
    val lastRiskScore: Int = 0,
    val lastAlertLevel: String = "SAFE",
    @ServerTimestamp val registeredAt: Timestamp? = null,
    @ServerTimestamp val lastSeen: Timestamp? = null
)
