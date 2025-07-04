package com.example.myapplication2.ble

data class Esp32Data(
    val rpm: Float = 0f,
    val engineTemp: Float = 0f,
    val fuelLevel: Float = 0f,
    val voltage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class BleConnectionState(
    val isConnected: Boolean = false,
    val deviceName: String? = null,
    val deviceAddress: String? = null
)