package com.example

import com.example.data.model.BleConnectionState
import com.example.data.model.DiscoveredBleDevice
import com.example.data.model.SafeAlertDevice
import com.example.data.model.SafeAlertUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun discoveredBleDevice_identifiesEsp32BeaconCorrectly() {
        val espDevice = DiscoveredBleDevice(
            name = "ESP32-C3 SAFEALERT",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -55,
            isEspOrSafeAlert = true
        )

        assertTrue(espDevice.isEspOrSafeAlert)
        assertEquals("ESP32-C3 SAFEALERT", espDevice.name)
        assertEquals("AA:BB:CC:DD:EE:FF", espDevice.address)
        assertEquals(-55, espDevice.rssi)
    }

    @Test
    fun discoveredBleDevice_genericBleDeviceNotMarkedAsEsp() {
        val genericDevice = DiscoveredBleDevice(
            name = "Fitness Tracker",
            address = "11:22:33:44:55:66",
            rssi = -82,
            isEspOrSafeAlert = false
        )

        assertFalse(genericDevice.isEspOrSafeAlert)
        assertEquals("Fitness Tracker", genericDevice.name)
        assertEquals(-82, genericDevice.rssi)
    }

    @Test
    fun safeAlertUiState_maintainsScanningAndDiscoveredDevicesList() {
        val devices = listOf(
            DiscoveredBleDevice("SAFEALERT-C3", "C4:4F:33:AA:BB:CC", -45, true),
            DiscoveredBleDevice("Unknown BLE", "00:11:22:33:44:55", -78, false)
        )

        val state = SafeAlertUiState(
            isScanning = true,
            bleConnectionState = BleConnectionState.SCANNING,
            discoveredDevices = devices
        )

        assertTrue(state.isScanning)
        assertEquals(2, state.discoveredDevices.size)
        assertEquals("SAFEALERT-C3", state.discoveredDevices[0].name)
        assertTrue(state.discoveredDevices[0].isEspOrSafeAlert)
    }
}
