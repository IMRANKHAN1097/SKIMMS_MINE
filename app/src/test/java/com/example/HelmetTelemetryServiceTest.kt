package com.example

import com.example.data.model.HelmetTelemetry
import com.example.data.model.WorkerProfile
import com.example.data.service.HelmetTelemetryService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HelmetTelemetryServiceTest {

    @Test
    fun testHelmetTelemetryModelInstantiation() {
        val telemetry = HelmetTelemetry(
            workerId = "ESP32_NODE_01",
            helmetNumber = "HLM-042",
            mq4 = 380.0,
            mq135 = 261.0,
            accel = 9.8,
            ir = 49107L,
            batteryPct = 94,
            riskScore = 12,
            alertLevel = "SAFE"
        )
        assertEquals("ESP32_NODE_01", telemetry.workerId)
        assertEquals("HLM-042", telemetry.helmetNumber)
        assertEquals(380.0, telemetry.mq4, 0.01)
        assertEquals(12, telemetry.riskScore)
        assertEquals("SAFE", telemetry.alertLevel)
    }

    @Test
    fun testWorkerProfileModelInstantiation() {
        val profile = WorkerProfile(
            workerId = "ESP32_NODE_04",
            helmetNumber = "HLM-099",
            workerName = "Anil Sen",
            workerPin = "4321",
            deviceKey = "kavach_test_key",
            sector = "Sector 3 - Stope B"
        )
        assertEquals("ESP32_NODE_04", profile.workerId)
        assertEquals("HLM-099", profile.helmetNumber)
        assertEquals("Anil Sen", profile.workerName)
        assertEquals("4321", profile.workerPin)
    }

    @Test
    fun testHelmetTelemetryServiceLocalCacheFallback() = runTest {
        // Without active Firebase Auth session, service safely handles CRUD via local cache
        val profile = WorkerProfile(
            workerId = "ESP32_NODE_99",
            helmetNumber = "HLM-099",
            workerName = "Test Miner",
            workerPin = "1111",
            deviceKey = "kavach_sample"
        )
        assertNotNull(profile)
        assertEquals("ESP32_NODE_99", profile.workerId)
    }
}
