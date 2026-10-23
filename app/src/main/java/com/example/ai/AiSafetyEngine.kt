package com.example.ai

import android.util.Log
import com.example.data.model.TelemetryData
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

data class SafetyEvaluationResult(
    val riskScore: Int,
    val alertLevel: String, // SAFE, CAUTION, DANGER, CRITICAL
    val reasons: List<String>,
    val predictedGasSpikeIn60s: Boolean,
    val isManDown: Boolean,
    val isHardwareFault: Boolean,
    val buzzerPeriodSec: Int, // 0 = off, 10 = missing sensor, 5 = health hazard, 2 = env hazard
    val ledState: String // GREEN_SOLID, YELLOW_SOLID, YELLOW_BLINK, RED_SOLID
)

class AiSafetyBoxEngine {
    companion object {
        const val MQ4_THRESHOLD = 2200.0   // Methane ADC limit
        const val MQ135_THRESHOLD = 2000.0 // Air Quality ADC limit
        const val IMPACT_THRESHOLD = 24.5  // m/s2
        const val REST_THRESHOLD = 10.5    // m/s2 (nominal earth gravity ~ 9.8)
    }

    // Historical buffer per worker to calculate trends and man-down
    private val historyMap = ConcurrentHashMap<String, MutableList<Pair<Long, Double>>>() // workerId -> list of (timestamp, mq4)
    private val impactTimestamps = ConcurrentHashMap<String, Long>() // workerId -> last impact timestamp

    fun evaluate(telemetry: TelemetryData, lastSeenMillis: Long = System.currentTimeMillis()): SafetyEvaluationResult {
        val now = System.currentTimeMillis()
        val reasons = mutableListOf<String>()

        // 1. Check signal lost (> 15s since last packet)
        val elapsedSec = (now - lastSeenMillis) / 1000
        if (elapsedSec >= 15) {
            return SafetyEvaluationResult(
                riskScore = 75,
                alertLevel = "DANGER",
                reasons = listOf("Signal lost: no telemetry packet received for ${elapsedSec}s."),
                predictedGasSpikeIn60s = false,
                isManDown = false,
                isHardwareFault = false,
                buzzerPeriodSec = 5,
                ledState = "YELLOW_BLINK"
            )
        }

        // 2. Hardware / Missing sensor check
        val sensorFault = telemetry.missing > 0 || !telemetry.mpuOk || !telemetry.maxOk || !telemetry.mq4Ok || !telemetry.mq135Ok
        if (sensorFault) {
            reasons.add("Hardware alert: ${telemetry.missing} sensor(s) offline/unreachable.")
        }

        // 3. Environmental Gas Hazard
        val mq4Hazard = telemetry.mq4 >= MQ4_THRESHOLD
        val mq135Hazard = telemetry.mq135 >= MQ135_THRESHOLD
        val envHazard = mq4Hazard || mq135Hazard || telemetry.envHazard
        if (mq4Hazard) reasons.add("MQ-4 Methane reading (${telemetry.mq4.toInt()}) exceeded safety limit ($MQ4_THRESHOLD).")
        if (mq135Hazard) reasons.add("MQ-135 Toxic Air reading (${telemetry.mq135.toInt()}) exceeded safety limit ($MQ135_THRESHOLD).")

        // 4. Gas Trend Prediction (Check if slope will cross limit within 60s)
        val workerHistory = historyMap.getOrPut(telemetry.workerId) { mutableListOf() }
        synchronized(workerHistory) {
            workerHistory.add(now to telemetry.mq4)
            // Keep last 30 seconds of history
            workerHistory.removeAll { (ts, _) -> now - ts > 30_000 }
        }

        var predictiveGasWarning = false
        if (workerHistory.size >= 3) {
            val first = workerHistory.first()
            val last = workerHistory.last()
            val dtSec = (last.first - first.first) / 1000.0
            val dGas = last.second - first.second
            if (dtSec > 2.0 && dGas > 50) {
                val ratePerSec = dGas / dtSec
                val projectedGasIn60s = last.second + (ratePerSec * 60.0)
                if (projectedGasIn60s >= MQ4_THRESHOLD && last.second < MQ4_THRESHOLD) {
                    predictiveGasWarning = true
                    reasons.add("AI Trend Warning: Methane rising at +${ratePerSec.toInt()} ADC/s, projected to breach hazard limit in < 60s!")
                }
            }
        }

        // 5. Physiological & Impact / Man-Down Logic
        val highImpact = telemetry.accel >= IMPACT_THRESHOLD
        if (highImpact) {
            impactTimestamps[telemetry.workerId] = now
            reasons.add("High acceleration impact recorded (${String.format("%.1f", telemetry.accel)} m/s²).")
        }

        var isManDown = false
        val lastImpact = impactTimestamps[telemetry.workerId]
        if (lastImpact != null) {
            val timeSinceImpact = (now - lastImpact) / 1000
            if (timeSinceImpact in 15..90 && telemetry.accel < REST_THRESHOLD) {
                isManDown = true
                reasons.add("MAN-DOWN DETECTED: Violent impact followed by immobility for ${timeSinceImpact}s.")
            }
        }

        // Pulse / optical IR sensor signal check
        val weakPulse = telemetry.ir in 1000..20000
        if (weakPulse) {
            reasons.add("MAX30105 Optical sensor: Weak pulse/physiological distress signal (IR count: ${telemetry.ir}).")
        }

        val healthHazard = highImpact || isManDown || weakPulse || telemetry.healthHazard

        // 6. Risk Score Calculation (0 - 100)
        val mq4Ratio = min(1.0, telemetry.mq4 / MQ4_THRESHOLD)
        val mq135Ratio = min(1.0, telemetry.mq135 / MQ135_THRESHOLD)
        val gasComponent = (max(mq4Ratio, mq135Ratio) * 60.0).toInt()
        val impactComponent = if (highImpact || isManDown) 35 else if (telemetry.accel > 15.0) 15 else 0
        val healthComponent = if (weakPulse) 20 else 0
        val faultComponent = if (sensorFault) 10 else 0

        var calculatedRisk = min(100, gasComponent + impactComponent + healthComponent + faultComponent)
        if (envHazard && healthHazard) calculatedRisk = 100
        if (isManDown) calculatedRisk = max(calculatedRisk, 95)
        if (predictiveGasWarning) calculatedRisk = max(calculatedRisk, 65)

        // 7. Determine Alert Level & Local Hardware Indicators (LED & Buzzer)
        val alertLevel: String
        val ledState: String
        val buzzerSec: Int

        when {
            (envHazard && healthHazard) || isManDown -> {
                alertLevel = "CRITICAL"
                ledState = "RED_SOLID"
                buzzerSec = 2 // 2s period for env/compound hazard
            }
            envHazard -> {
                alertLevel = "DANGER"
                ledState = "RED_SOLID"
                buzzerSec = 2
            }
            healthHazard -> {
                alertLevel = "DANGER"
                ledState = if (sensorFault) "YELLOW_BLINK" else "YELLOW_SOLID"
                buzzerSec = 5 // 5s period for health hazard
            }
            predictiveGasWarning -> {
                alertLevel = "CAUTION"
                ledState = "YELLOW_SOLID"
                buzzerSec = 5
            }
            sensorFault -> {
                alertLevel = "CAUTION"
                ledState = "YELLOW_BLINK"
                buzzerSec = 10 // 10s period if sensor is missing
            }
            else -> {
                alertLevel = "SAFE"
                ledState = "GREEN_SOLID"
                buzzerSec = 0 // silent
                if (reasons.isEmpty()) {
                    reasons.add("Atmospheric gases and vital signs are within safe operating limits.")
                }
            }
        }

        return SafetyEvaluationResult(
            riskScore = calculatedRisk,
            alertLevel = alertLevel,
            reasons = reasons,
            predictedGasSpikeIn60s = predictiveGasWarning,
            isManDown = isManDown,
            isHardwareFault = sensorFault,
            buzzerPeriodSec = buzzerSec,
            ledState = ledState
        )
    }
}

class GeminiMineSafetyAdvisor {
    private val TAG = "GeminiMineAdvisor"

    // High thinking model configuration per required metadata:
    // Model: gemini-4-argon with thinkingLevel: ThinkingLevel.HIGH, no maxOutputTokens
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    suspend fun analyzeMineIncident(
        workerName: String,
        helmetId: String,
        sector: String,
        telemetry: TelemetryData,
        alertReason: String
    ): Result<String> {
        return try {
            val prompt = """
                You are the Chief Mine Safety Officer and AI Incident Diagnostician for SKIMMS (Team HEXACREW, University of Calcutta).
                Perform a deep, step-by-step reasoning analysis of the following mine hazard incident:
                
                Worker: $workerName
                Helmet Node ID: ${telemetry.workerId} (Tag: $helmetId)
                Underground Mine Sector: $sector
                GPS Coordinates: Lat ${telemetry.lat}, Lng ${telemetry.lng}
                Telemetry Data:
                - MQ-4 Methane Raw ADC: ${telemetry.mq4} (Hazard limit: 2200)
                - MQ-135 Toxic Air Raw ADC: ${telemetry.mq135} (Hazard limit: 2000)
                - Acceleration: ${telemetry.accel} m/s² (Earth gravity baseline: ~9.8 m/s²)
                - MAX30105 Optical IR Pulse: ${telemetry.ir} (Weak threshold: 1000-20000)
                - Battery: ${telemetry.batteryPct}%
                - Missing Sensors: ${telemetry.missing}
                - Incident Alert: $alertReason
                
                Please structure your response with:
                1. ROOT-CAUSE HAZARD CLASSIFICATION (Combustible gas pocket, toxic fume accumulation, structural collapse/rockfall, or physiological distress).
                2. SPREAD DYNAMICS & VENTILATION RISK (How air velocity and airway geometry could propagate the hazard to nearby shafts).
                3. IMMEDIATE ACTION & CONTROL ROOM DIRECTIVES (Ventilation reversal, power isolation, rescue squad dispatch protocol).
                4. SAFE RETREAT & EVACUATION CORRIDOR ADVISORY for the miner.
                
                Provide concise, authoritative, professional safety engineering advice.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Model returned empty analysis."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini safety analysis failed", e)
            Result.failure(e)
        }
    }
}
