package com.example.myapplication2.ble

import android.bluetooth.*
import android.bluetooth.le.*
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class Esp32BleService(private val context: Context) {
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    
    // UUIDs for ESP32 communication
    companion object {
        private const val TAG = "Esp32BleService"
        private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        private val RPM_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abd")
        private val TEMP_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abe")
        private val FUEL_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abf")
    }
    
    // Flow for ESP32 data
    private val _esp32Data = MutableStateFlow(Esp32Data())
    val esp32Data: StateFlow<Esp32Data> = _esp32Data.asStateFlow()
    
    // Flow for connection state
    private val _connectionState = MutableStateFlow(BleConnectionState())
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    // Scanner
    private val bleScanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            
            // Look for ESP32 device by name
            if (device.name?.contains("ESP32", ignoreCase = true) == true) {
                Log.d(TAG, "Found ESP32 device: ${device.name} - ${device.address}")
                stopScanning()
                connectToDevice(device)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
            isScanning = false
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    _connectionState.value = BleConnectionState(
                        isConnected = true,
                        deviceName = gatt?.device?.name,
                        deviceAddress = gatt?.device?.address
                    )
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectionState.value = BleConnectionState(isConnected = false)
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                enableNotifications(gatt)
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.let {
                handleCharacteristicUpdate(it)
            }
        }
    }
    
    fun startScanning() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        if (!isScanning) {
            isScanning = true
            val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
                
            bleScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            Log.d(TAG, "Started BLE scan")
        }
    }
    
    fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        if (isScanning) {
            bleScanner?.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "Stopped BLE scan")
        }
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
    
    private fun enableNotifications(gatt: BluetoothGatt?) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val service = gatt?.getService(SERVICE_UUID)
        
        // Enable notifications for each characteristic
        service?.getCharacteristic(RPM_CHARACTERISTIC_UUID)?.let { characteristic ->
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            descriptor?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
            }
        }
        
        service?.getCharacteristic(TEMP_CHARACTERISTIC_UUID)?.let { characteristic ->
            gatt.setCharacteristicNotification(characteristic, true)
        }
        
        service?.getCharacteristic(FUEL_CHARACTERISTIC_UUID)?.let { characteristic ->
            gatt.setCharacteristicNotification(characteristic, true)
        }
    }

    private fun handleCharacteristicUpdate(characteristic: BluetoothGattCharacteristic) {
        val data = characteristic.value
        
        when (characteristic.uuid) {
            RPM_CHARACTERISTIC_UUID -> {
                val rpm = data?.let { parseFloat(it) } ?: 0f
                _esp32Data.value = _esp32Data.value.copy(rpm = rpm)
            }
            TEMP_CHARACTERISTIC_UUID -> {
                val temp = data?.let { parseFloat(it) } ?: 0f
                _esp32Data.value = _esp32Data.value.copy(engineTemp = temp)
            }
            FUEL_CHARACTERISTIC_UUID -> {
                val fuel = data?.let { parseFloat(it) } ?: 0f
                _esp32Data.value = _esp32Data.value.copy(fuelLevel = fuel)
            }
        }
    }
    
    private fun parseFloat(data: ByteArray): Float {
        return try {
            if (data.size >= 4) {
                // If data is 4 bytes, treat as float
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).float
            } else {
                // If data is string, parse as string
                String(data).toFloat()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing float from BLE data", e)
            0f
        }
    }

    
    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        stopScanning()
    }
}