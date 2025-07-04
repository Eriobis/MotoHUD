# Automotive Dashboard App

A comprehensive Android automotive dashboard application with ESP32 integration, featuring real-time gauges, Mapbox navigation, and BLE connectivity.

## Features

### 📱 **Core Functionality**
- **Fullscreen Landscape Mode**: Optimized for automotive use
- **Jetpack Compose UI**: Modern, fluid animations with `animateFloatAsState`
- **Real-time Data Display**: Live gauges for speed and RPM
- **BLE Connectivity**: Direct communication with ESP32 modules
- **Advanced Location Services**: High-accuracy GPS with 1-second intervals

### 🗺️ **Mapping & Navigation**
- **Mapbox Integration**: High-quality maps with offline region support
- **Speed Limit API**: Real-time posted speed limits via MapboxSpeedInfoApi
- **Live Speed Tracking**: Accurate speed calculation from GPS
- **Location Following**: Auto-centering on current position

### 🔧 **ESP32 Integration**
- **BLE Communication**: Using kotlinx.coroutines.flow for reactive data streams
- **Real-time Engine Data**:
  - RPM monitoring (0-12,000 RPM with redline at 6,500)
  - Engine temperature
  - Fuel level percentage
  - Battery voltage
- **Connection Status**: Visual indicators for BLE connectivity

### ⚡ **Power Management**
- **Screen Always On**: `FLAG_KEEP_SCREEN_ON` prevents screen timeout
- **Auto-brightness**: 100% brightness when connected to USB power
- **Battery Monitoring**: Automatic brightness adjustment based on power source

## Technical Architecture

### 🏗️ **Project Structure**
```
app/src/main/java/com/example/myapplication2/
├── ble/
│   ├── BleData.kt              # Data classes for ESP32 communication
│   └── Esp32BleService.kt      # BLE service with coroutines
├── location/
│   └── LocationService.kt      # FusedLocationProvider wrapper
├── ui/
│   ├── compose/
│   │   ├── DashboardScreen.kt  # Main dashboard UI
│   │   └── Gauge.kt           # Animated gauge components
│   ├── theme/
│   │   ├── Theme.kt           # Material 3 dark theme
│   │   └── Type.kt            # Typography definitions
│   └── viewmodel/
│       └── DashboardViewModel.kt # State management
└── MainActivity.kt             # Entry point with power management
```

### 📊 **Data Flow**
1. **ESP32 BLE** → `Esp32BleService` → `StateFlow<Esp32Data>`
2. **GPS Location** → `LocationService` → `StateFlow<LocationData>`
3. **ViewModel** → Combines all data streams
4. **Compose UI** → Reacts to state changes with animations

### 🔌 **ESP32 BLE Protocol**
- **Service UUID**: `12345678-1234-1234-1234-123456789abc`
- **Characteristics**:
  - RPM: `12345678-1234-1234-1234-123456789abd`
  - Temperature: `12345678-1234-1234-1234-123456789abe`
  - Fuel: `12345678-1234-1234-1234-123456789abf`

## Dependencies

### 📦 **Key Libraries**
```kotlin
// Compose BOM for UI
implementation(platform("androidx.compose:compose-bom:2024.12.01"))

// Location Services
implementation("com.google.android.gms:play-services-location:21.3.0")

// Mapbox Maps
implementation("com.mapbox.maps:android:11.5.1")

// BLE Communication
implementation("no.nordicsemi.android:ble:2.7.6")

// Coroutines for reactive streams
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## Setup Instructions

### 🚀 **Getting Started**

1. **Clone and Build**:
   ```bash
   git clone <repository>
   ./gradlew build
   ```

2. **Mapbox Configuration**:
   - Get API key from [Mapbox](https://account.mapbox.com/)
   - Add to `local.properties`: `MAPBOX_ACCESS_TOKEN=pk.your_token_here`

3. **ESP32 Setup**:
   - Program ESP32 with BLE server code
   - Use the defined UUIDs for characteristics
   - Send float values as strings over BLE

4. **Permissions**:
   - Location permissions for GPS
   - Bluetooth permissions for BLE
   - All handled automatically with runtime requests

### 🎛️ **Dashboard Layout**

**Top Row**:
- **Left**: Speed gauge (0-200 km/h)
- **Center**: Speed limit indicator (when available)
- **Right**: RPM gauge (0-12,000 RPM, redline at 6,500)

**Background**: Full-screen Mapbox map

**Bottom Row**: Status indicators
- BLE connection status
- Engine temperature
- Fuel level
- Battery voltage

### 🔧 **Customization**

**Gauge Colors**:
- Green: Normal operation
- Yellow: Warning range (>80% of max)
- Red: Critical/redline values

**Map Features**:
- Automatic offline region downloading
- Speed limit overlay
- Real-time position tracking

## ESP32 Integration Guide

### 📡 **BLE Server Code** (Arduino)
```cpp
#include "BLEDevice.h"
#include "BLEServer.h"
#include "BLEUtils.h"
#include "BLE2902.h"

#define SERVICE_UUID        "12345678-1234-1234-1234-123456789abc"
#define RPM_CHAR_UUID       "12345678-1234-1234-1234-123456789abd"
#define TEMP_CHAR_UUID      "12345678-1234-1234-1234-123456789abe"
#define FUEL_CHAR_UUID      "12345678-1234-1234-1234-123456789abf"

BLEServer* pServer = NULL;
BLECharacteristic* pRpmCharacteristic = NULL;
BLECharacteristic* pTempCharacteristic = NULL;
BLECharacteristic* pFuelCharacteristic = NULL;

void setup() {
  BLEDevice::init("ESP32-Dashboard");
  pServer = BLEDevice::createServer();
  
  BLEService *pService = pServer->createService(SERVICE_UUID);
  
  pRpmCharacteristic = pService->createCharacteristic(
                        RPM_CHAR_UUID,
                        BLECharacteristic::PROPERTY_READ |
                        BLECharacteristic::PROPERTY_NOTIFY
                      );
                      
  // Setup other characteristics...
  pService->start();
  pServer->getAdvertising()->start();
}

void loop() {
  // Read sensor data
  float rpm = readRPM();
  float temp = readEngineTemp();
  float fuel = readFuelLevel();
  
  // Send via BLE
  String rpmStr = String(rpm);
  pRpmCharacteristic->setValue(rpmStr.c_str());
  pRpmCharacteristic->notify();
  
  delay(100); // 10Hz update rate
}
```

## Performance Optimizations

- **Coroutine-based BLE**: Non-blocking communication
- **Efficient Location Updates**: 1-second intervals with smart filtering
- **Compose Animations**: Hardware-accelerated gauge animations
- **Memory Management**: Proper lifecycle handling for BLE and location services

## Future Enhancements

- **OBD-II Integration**: Direct vehicle diagnostics
- **Navigation Integration**: Turn-by-turn directions
- **Trip Computer**: Distance, fuel consumption, trip times
- **Data Logging**: Historical performance data
- **Customizable Dashboard**: User-configurable gauge layouts