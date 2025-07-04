package com.example.myapplication2.ble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.*
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
    private var isReading = false
    
    // Coroutine scope for periodic reading
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readingJob: Job? = null
    
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
            Log.d(TAG, "BLE device found: ${device.name} - ${device.address}")
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            
            // Look for ESP32 device by name
            if (device.name?.contains("ESP32", ignoreCase = true) == true) {
                Log.d(TAG, "Found ESP32 device: ${device.name} - ${device.address}")
                _connectionState.value = BleConnectionState(
                    isConnected = false,
                    deviceName = device.name,
                    deviceAddress = device.address,
                    status = "FOUND DEVICE"
                )
                stopScanning()
                connectToDevice(device)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
            isScanning = false
            _connectionState.value = BleConnectionState(
                isConnected = false,
                status = "SCAN FAILED"
            )
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTING -> {
                    Log.d(TAG, "Connecting to GATT server")
                    _connectionState.value = BleConnectionState(
                        isConnected = false,
                        deviceName = gatt?.device?.name,
                        deviceAddress = gatt?.device?.address,
                        status = "CONNECTING"
                    )
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    _connectionState.value = BleConnectionState(
                        isConnected = false, // Not fully connected until services are discovered
                        deviceName = gatt?.device?.name,
                        deviceAddress = gatt?.device?.address,
                        status = "DISCOVERING"
                    )
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectionState.value = BleConnectionState(
                        isConnected = false,
                        status = "DISCONNECTED"
                    )
                }
            }
        }
        
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")

                _connectionState.value = BleConnectionState(
                    isConnected = true,
                    deviceName = gatt?.device?.name,
                    deviceAddress = gatt?.device?.address,
                    status = "CONNECTED"
                )
                startPeriodicReading(gatt)
            } else {
                Log.e(TAG, "Service discovery failed")
                _connectionState.value = BleConnectionState(
                    isConnected = false,
                    status = "SERVICE ERROR"
                )
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.let {
                handleCharacteristicUpdate(it)
            }
        }
        
        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.let {
                    handleCharacteristicUpdate(it)
                }
            } else {
                Log.e(TAG, "Characteristic read failed for ${characteristic?.uuid} with status: $status")
            }
        }
    }
    
    fun startScanning() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            _connectionState.value = BleConnectionState(
                isConnected = false,
                status = "NO PERMISSION"
            )
            return
        }
        
        if (!isScanning) {
            isScanning = true
            _connectionState.value = BleConnectionState(
                isConnected = false,
                status = "SCANNING"
            )
            
            val scanFilter = ScanFilter.Builder()
//                .setServiceUuid(ParcelUuid(SERVICE_UUID))
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
            _connectionState.value = BleConnectionState(
                isConnected = false,
                status = "NO PERMISSION"
            )
            return
        }
        
        _connectionState.value = BleConnectionState(
            isConnected = false,
            deviceName = device.name,
            deviceAddress = device.address,
            status = "CONNECTING"
        )
        
        bluetoothGatt = device.connectGatt(context, true, gattCallback)
    }
    
    private fun startPeriodicReading(gatt: BluetoothGatt?) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val service = gatt?.getService(SERVICE_UUID)
        if (service == null) {
            Log.e(TAG, "Service not found!")
            return
        }
        
        Log.d(TAG, "Starting periodic reading of characteristics")
        isReading = true
        
        readingJob = serviceScope.launch {
            while (isReading && gatt != null) {
                try {
                    // Read RPM
                    service.getCharacteristic(RPM_CHARACTERISTIC_UUID)?.let { characteristic ->
                        gatt.readCharacteristic(characteristic)
                        delay(100) // Small delay between reads
                    }
                    
                    // Read Temperature
                    service.getCharacteristic(TEMP_CHARACTERISTIC_UUID)?.let { characteristic ->
                        gatt.readCharacteristic(characteristic)
                        delay(100)
                    }
                    
                    // Read Fuel
                    service.getCharacteristic(FUEL_CHARACTERISTIC_UUID)?.let { characteristic ->
                        gatt.readCharacteristic(characteristic)
                        delay(100)
                    }
                    
                    // Wait before next cycle (read every 500ms total)
                    delay(200)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error during periodic reading", e)
                    break
                }
            }
        }
    }
    
    private fun stopPeriodicReading() {
        isReading = false
        readingJob?.cancel()
        readingJob = null
        Log.d(TAG, "Stopped periodic reading")
    }

    private fun handleCharacteristicUpdate(characteristic: BluetoothGattCharacteristic) {
        @Suppress("DEPRECATION")
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
        
        stopPeriodicReading()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        stopScanning()
    }
}