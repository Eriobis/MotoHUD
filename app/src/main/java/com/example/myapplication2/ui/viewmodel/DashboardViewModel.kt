package com.example.myapplication2.ui.viewmodel

/**
 * DashboardViewModel - Manages automotive dashboard state
 *
 * DEBUG MODE:
 * - Set isDebugMode = true to use mock ESP32 data for testing without hardware
 * - Mock data includes realistic RPM (800-6000), temperature (70-110°C),
 *   fuel (10-100%), and voltage (11-14.5V) with smooth sine wave animations
 *
 * PRODUCTION MODE:
 * - Set isDebugMode = false to use real ESP32 BLE communication
 */

import android.app.Application
import android.content.Context
import android.location.Location
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.ble.BleConnectionState
import com.example.myapplication2.ble.Esp32BleService
import com.example.myapplication2.ble.Esp32Data
import com.example.myapplication2.location.LocationService
import com.google.android.gms.location.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LocationData(
    val location: Location? = null,
    val speedKmh: Float = 0f,
    val bearing: Float = 0f
)

data class TripData(
    val distance: Float = 0f, // km
    val time: Long = 0L, // milliseconds since trip start
    val avgSpeed: Float = 0f // km/h
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = getApplication<Application>()
    private val bleService = Esp32BleService(context)
    private val locationService = LocationService(context)
    
    // Debug mode for testing without ESP32
    private val isDebugMode = false  // Set to false when ESP32 is available
    
    // Mock ESP32 data for debugging
    private val _mockEsp32Data = MutableStateFlow(Esp32Data())
    private val _mockConnectionState = MutableStateFlow(
        BleConnectionState(
            isConnected = isDebugMode,
            deviceName = "ESP32-Mock",
            status = if (isDebugMode) "CONNECTED" else "IDLE"
        )
    )
    
    // BLE data flows - use mock data in debug mode
    val esp32Data: StateFlow<Esp32Data> = if (isDebugMode) _mockEsp32Data.asStateFlow() else bleService.esp32Data
    val connectionState: StateFlow<BleConnectionState> = if (isDebugMode) _mockConnectionState.asStateFlow() else bleService.connectionState
    
    // Location data flow
    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData.asStateFlow()
    
    // Speed limit from Mapbox
    private val _speedLimit = MutableStateFlow(0)
    val speedLimit: StateFlow<Int> = _speedLimit.asStateFlow()
    
    // Trip meter data
    private val _tripData = MutableStateFlow(TripData())
    val tripData: StateFlow<TripData> = _tripData.asStateFlow()
    
    // Trip meter tracking variables
    private var tripStartTime: Long = System.currentTimeMillis()
    private var totalTripDistance: Float = 0f
    private var lastTripLocation: Location? = null
    
    // Location request configuration
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
        .setWaitForAccurateLocation(false)
        .setMinUpdateIntervalMillis(500)
        .setMaxUpdateDelayMillis(2000)
        .build()
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d("LocationService", "=== LOCATION RECEIVED ===")
            Log.d("LocationService", "Location count: ${locationResult.locations.size}")
            
            locationResult.lastLocation?.let { location ->
                Log.d("LocationService", "Location: ${location.latitude}, ${location.longitude}")
                Log.d("LocationService", "Accuracy: ${location.accuracy}m")
                Log.d("LocationService", "Has speed: ${location.hasSpeed()}")
                
                val speedKmh = if (location.hasSpeed() && (location.speed * 3.6f) > 4) {
                    location.speed * 3.6f // m/s to km/h
                } else {
                    calculateSpeedFromPreviousLocation(location)
                }
                
                Log.d("LocationService", "Speed: ${speedKmh} km/h")
                
                _locationData.value = LocationData(
                    location = location,
                    speedKmh = speedKmh,
                    bearing = if (location.hasBearing()) location.bearing else 0f
                )
                
                // Update trip meter
                updateTripMeter(location)
                
                // Fetch speed limit for current location
                fetchSpeedLimit(location.latitude, location.longitude)
            } ?: Log.e("LocationService", "Location is null!")
        }
        
        override fun onLocationAvailability(availability: LocationAvailability) {
            Log.d("LocationService", "Location availability: ${availability.isLocationAvailable}")
        }
    }
    
    private var previousLocation: Location? = null
    private var previousTime: Long = 0
    
    init {
        startLocationUpdates()
        if (isDebugMode) {
            startMockDataGeneration()
        } else {
            startBleScanning()
        }
    }
    
    private fun startLocationUpdates() {
        Log.d("LocationService", "=== STARTING LOCATION UPDATES ===")
        try {
            locationService.startLocationUpdates(locationRequest, locationCallback)
            Log.d("LocationService", "Location service started successfully")
        } catch (e: Exception) {
            Log.e("LocationService", "Failed to start location service", e)
        }
    }
    
    private fun startBleScanning() {
        viewModelScope.launch {
            bleService.startScanning()
        }
    }
    
    private fun calculateSpeedFromPreviousLocation(currentLocation: Location): Float {
        val currentTime = System.currentTimeMillis()
        
        return previousLocation?.let { prevLoc ->
            if (previousTime > 0) {
                val distance = prevLoc.distanceTo(currentLocation) // meters
                val timeDiff = (currentTime - previousTime) / 1000.0 // seconds
                
                if (timeDiff > 0) {
                    val speedMs = distance / timeDiff // m/s
                    val speedKmh = speedMs * 3.6 // km/h
                    
                    // Update previous location and time
                    previousLocation = currentLocation
                    previousTime = currentTime
                    
                    speedKmh.toFloat()
                } else {
                    0f
                }
            } else {
                previousLocation = currentLocation
                previousTime = currentTime
                0f
            }
        } ?: run {
            previousLocation = currentLocation
            previousTime = currentTime
            0f
        }
    }
    
    private fun fetchSpeedLimit(latitude: Double, longitude: Double) {
        // TODO: Implement Mapbox Speed Info API
        // For now, simulate speed limits based on typical road types
        viewModelScope.launch {
            try {
                // This would normally call Mapbox Speed Info API
                // val speedInfo = mapboxSpeedInfoApi.getSpeedLimit(latitude, longitude)
                // _speedLimit.value = speedInfo.speedLimit
                
                // Simulate speed limit (replace with actual API call)
                val simulatedLimit = when {
                    // Urban areas (simplified logic)
                    true -> 50 // Default urban speed limit
                    else -> 80 // Highway speed limit
                }
                _speedLimit.value = simulatedLimit
            } catch (e: Exception) {
                // Handle API error
                _speedLimit.value = 0
            }
        }
    }
    
    fun updateMapLocation(mapView: MapView, latitude: Double, longitude: Double) {
        val geoPoint = GeoPoint(latitude, longitude)
        mapView.controller.setCenter(geoPoint)
        mapView.controller.setZoom(16.0)
    }
    
    fun adjustBrightness(isOnUSBPower: Boolean) {
        try {
            val brightness = if (isOnUSBPower) 255 else Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            
            // This would need to be called from an Activity context
            // For now, we'll handle it in the MainActivity
        } catch (e: Exception) {
            // Handle permission error
        }
    }
    
    private fun startMockDataGeneration() {
        viewModelScope.launch {
            var time = 0f
            while (true) {
                kotlinx.coroutines.delay(100) // Update every 100ms for smooth animations
                time += 0.1f
                
                // Generate realistic mock data with sine waves for smooth animation
                val baseRpm = 500f
                val rpmVariation = 2500f * kotlin.math.sin(time * 0.5f)
                val rpm = (baseRpm + rpmVariation).coerceIn(800f, 10000f)
                
                val engineTemp = 85f + 10f * kotlin.math.sin(time * 0.2f)
                val fuelLevel = 75f + 15f * kotlin.math.sin(time * 0.1f)
                val voltage = 12.6f + 0.8f * kotlin.math.sin(time * 0.3f)
                
                _mockEsp32Data.value = Esp32Data(
                    rpm = rpm,
                    engineTemp = engineTemp.coerceIn(70f, 110f),
                    fuelLevel = fuelLevel.coerceIn(10f, 100f),
                    voltage = voltage.coerceIn(11.0f, 14.5f)
                )
            }
        }
    }
    

    private fun updateTripMeter(location: Location) {
        lastTripLocation?.let { lastLoc ->
            val distance = lastLoc.distanceTo(location) / 1000f // Convert to km
            totalTripDistance += distance
            
            val tripTime = System.currentTimeMillis() - tripStartTime
            val avgSpeed = if (tripTime > 0) {
                (totalTripDistance / (tripTime / 3600000f)) // km/h
            } else 0f
            
            _tripData.value = TripData(
                distance = totalTripDistance,
                time = tripTime,
                avgSpeed = avgSpeed
            )
        }
        lastTripLocation = location
    }
    
    fun resetTripMeter() {
        tripStartTime = System.currentTimeMillis()
        totalTripDistance = 0f
        lastTripLocation = null
        _tripData.value = TripData()
    }
    
    fun centerMapOnLocation() {
        // This will be called from the UI to trigger map centering
        _locationData.value.location?.let { location ->
            // The UI will observe this and center the map
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        if (!isDebugMode) {
            bleService.disconnect()
        }
        locationService.stopLocationUpdates(locationCallback)
    }
}