package com.example

import com.example.ai.AiSafetyBoxEngine
import com.example.data.model.TelemetryData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiSafetyBoxEngineTest {
    private lateinit var engine: AiSafetyBoxEngine

    @Before
    fun setup() {
        engine = AiSafetyBoxEngine()
    }

    @Test
    fun testNormalAllClear() {
        val normal = TelemetryData(
            workerId = "ESP32_NODE_01",
            mq4 = 380.0,
            mq135 = 261.0,
            accel = 9.8,
            ir = 49107L,
            missing = 0
        )
        val result = engine.evaluate(normal)
        assertEquals("SAFE", result.alertLevel)
        assertEquals("GREEN_SOLID", result.ledState)
        assertEquals(0, result.buzzerPeriodSec)
        assertTrue(result.riskScore < 25)
    }

    @Test
    fun testMethaneHazardThreshold() {
        val gasSpike = TelemetryData(
            workerId = "ESP32_NODE_01",
            mq4 = 2350.0, // >= 2200
            mq135 = 261.0,
            accel = 9.8,
            ir = 49107L
        )
        val result = engine.evaluate(gasSpike)
        assertEquals("DANGER", result.alertLevel)
        assertEquals("RED_SOLID", result.ledState)
        assertEquals(2, result.buzzerPeriodSec)
        assertTrue(result.reasons.any { it.contains("Methane") })
    }

    @Test
    fun testToxicAirQualityHazardThreshold() {
        val toxicSpike = TelemetryData(
            workerId = "ESP32_NODE_01",
            mq4 = 380.0,
            mq135 = 2150.0, // >= 2000
            accel = 9.8,
            ir = 49107L
        )
        val result = engine.evaluate(toxicSpike)
        assertEquals("DANGER", result.alertLevel)
        assertEquals("RED_SOLID", result.ledState)
        assertEquals(2, result.buzzerPeriodSec)
    }

    @Test
    fun testWeakPulseOpticalDistress() {
        val weakPulse = TelemetryData(
            workerId = "ESP32_NODE_01",
            mq4 = 380.0,
            mq135 = 261.0,
            accel = 9.8,
            ir = 8500L // in 1000..20000
        )
        val result = engine.evaluate(weakPulse)
        assertEquals("DANGER", result.alertLevel)
        assertEquals("YELLOW_SOLID", result.ledState)
        assertEquals(5, result.buzzerPeriodSec)
        assertTrue(result.reasons.any { it.contains("Weak pulse") })
    }

    @Test
    fun testMissingSensorHardwareFault() {
        val fault = TelemetryData(
            workerId = "ESP32_NODE_01",
            mq4 = 380.0,
            mq135 = 261.0,
            accel = 9.8,
            ir = 49107L,
            missing = 1,
            mpuOk = false
        )
        val result = engine.evaluate(fault)
        assertEquals("CAUTION", result.alertLevel)
        assertEquals("YELLOW_BLINK", result.ledState)
        assertEquals(10, result.buzzerPeriodSec)
    }

    @Test
    fun testCompoundGasAndHealthHazardIsCritical() {
        val compound = TelemetryData(
            workerId = "ESP32_NODE_01",
            mq4 = 2400.0, // gas hazard
            mq135 = 261.0,
            accel = 28.0, // fall impact hazard
            ir = 49107L
        )
        val result = engine.evaluate(compound)
        assertEquals("CRITICAL", result.alertLevel)
        assertEquals("RED_SOLID", result.ledState)
        assertEquals(2, result.buzzerPeriodSec)
        assertEquals(100, result.riskScore)
    }

    @Test
    fun testTelemetryJsonParser() {
        val jsonStr = """{"board_id":"ESP32_NODE_01","gps":{"valid":true,"lat":22.57,"lng":88.36},"sensors":{"mq4":380,"mq135":261,"accel":9.68,"ir":49107},"status":{"mpu_ok":true,"max_ok":true,"mq4_ok":true,"mq135_ok":true,"health_hazard":false,"env_hazard":false,"missing":0}}"""
        val parsed = TelemetryData.fromJson(jsonStr, "HLM-042")
        assertEquals("ESP32_NODE_01", parsed.workerId)
        assertEquals("HLM-042", parsed.helmetNumber)
        assertEquals(380.0, parsed.mq4, 0.01)
        assertEquals(261.0, parsed.mq135, 0.01)
        assertEquals(9.68, parsed.accel, 0.01)
        assertEquals(49107L, parsed.ir)
        assertTrue(parsed.mpuOk)
        assertEquals(0, parsed.missing)
    }
}
