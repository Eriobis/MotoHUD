package com.example.myapplication2.ui.compose

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import com.example.myapplication2.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication2.ui.viewmodel.DashboardViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.util.Log
import androidx.compose.ui.Alignment
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val esp32Data by viewModel.esp32Data.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val speedLimit by viewModel.speedLimit.collectAsState()
    val tripData by viewModel.tripData.collectAsState()
    
    // Store map reference for centering
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    
    // Monitor USB power status
    var isOnUSBPower by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        isOnUSBPower = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
    }
    
    // Adjust brightness based on USB power
    LaunchedEffect(isOnUSBPower) {
        viewModel.adjustBrightness(isOnUSBPower)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background map (full screen)
        AndroidView(
            factory = { context ->
                // Initialize OSMDroid configuration
                val prefs = context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
                Configuration.getInstance().load(context, prefs)

                Configuration.getInstance().userAgentValue = context.packageName
                
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    
                    // Set default location (Toronto) initially
                    val defaultLocation = GeoPoint(43.6532, -79.3832)
                    controller.setCenter(defaultLocation)
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { mapView ->
            // Always update map reference
            mapViewRef = mapView
            
            // Update map with current location
            locationData.location?.let { location ->
                viewModel.updateMapLocation(mapView, location.latitude, location.longitude)
            }
            
            // Add location overlay for current position
            val mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
            mLocationOverlay.enableMyLocation()
            if (!mapView.overlays.contains(mLocationOverlay)) {
                mapView.overlays.add(mLocationOverlay)
            }
        }
        
        // Top row - Gauges and speed limit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            SpeedCard(
                title = "Speed (km/h)",
                value = locationData.speedKmh.toString(),
                color = Color.White
            )
            
            // Speed limit indicator
            if (speedLimit > 0) {
                Card(
                    modifier = Modifier.size(80.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$speedLimit",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "LIMIT",
                                fontSize = 10.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
            
            // RPM gauge
            Card(
                modifier = Modifier.width(200.dp).height(120.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Gauge(
                        value = esp32Data.rpm,
                        unit = "RPM",
                        max = 12000f,
                        redlineValue = 9500f
                    )
                }
            }
        }
        
        // Bottom row - Status indicators and controls
        Row(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.9f)
                .align(Alignment.BottomStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Connection status
            StatusCard(
                title = "BLE",
                value = connectionState.status,
                color = when {
                    connectionState.isConnected -> Color.Green
                    connectionState.status == "SCANNING" -> Color.Yellow
                    connectionState.status == "CONNECTING" -> Color.Yellow
                    connectionState.status == "DISCOVERING" -> Color.Yellow
                    connectionState.status == "FOUND DEVICE" -> Color.Cyan
                    else -> Color.Red
                }
            )
            
            // Engine temp
            StatusCard(
                title = "TEMP",
                value = "${esp32Data.engineTemp.toInt()}°C",
                color = when {
                    esp32Data.engineTemp > 100 -> Color.Red
                    esp32Data.engineTemp > 85 -> Color.Yellow
                    else -> Color.Green
                }
            )
            
            // Trip meter
            StatusCard(
                title = "TRIP",
                value = "${String.format(Locale.getDefault(), "%.1f", tripData.distance)} km" ,
                color = Color.Cyan
            )
            
            // Fuel level
            StatusCard(
                title = "FUEL",
                value = "${esp32Data.fuelLevel.toInt()}%",
                color = when {
                    esp32Data.fuelLevel < 20 -> Color.Red
                    esp32Data.fuelLevel < 40 -> Color.Yellow
                    else -> Color.Green
                }
            )
            
            // Voltage
            StatusCard(
                title = "VOLTS",
                value = "${String.format(Locale.getDefault(), "%.1f", esp32Data.voltage)}V",
                color = when {
                    esp32Data.voltage < 11.5 -> Color.Red
                    esp32Data.voltage < 12.0 -> Color.Yellow
                    else -> Color.Green
                }
            )
        }
        
        // Control buttons (bottom right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Center map button
            FloatingActionButton(
                onClick = {
                    Log.d("CenterButton", "Center button clicked!")
                    Log.d("CenterButton", "MapView ref: $mapViewRef")
                    Log.d("CenterButton", "Location data: ${locationData.location}")
                    
                    // Try multiple approaches
                    when {
                        locationData.location != null && mapViewRef != null -> {
                            val location = locationData.location!!
                            val mapView = mapViewRef!!
                            Log.d("CenterButton", "Using location data: ${location.latitude}, ${location.longitude}")
                            
                            val geoPoint = GeoPoint(location.latitude, location.longitude)
                            mapView.controller.setCenter(geoPoint)
//                            mapView.controller.setZoom(16.0)
                            mapView.invalidate()
                            Log.d("CenterButton", "Map centered to location")
                        }
                        mapViewRef != null -> {
                            val mapView = mapViewRef!!
                            Log.d("CenterButton", "No location data, trying default")
                            
                            // Try to use default location (Toronto)
                            val defaultLocation = GeoPoint(43.6532, -79.3832)
                            mapView.controller.setCenter(defaultLocation)
//                            mapView.controller.setZoom(15.0)
                            mapView.invalidate()
                            Log.d("CenterButton", "Map centered to default location")
                        }
                        else -> {
                            Log.e("CenterButton", "No map reference available")
                        }
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Center Map",
                    tint = Color.Black
                )
            }
            
            // Trip reset button (hold to reset)
            var isHoldingTripReset by remember { mutableStateOf(false) }
            
            FloatingActionButton(
                onClick = { /* Single tap does nothing */ },
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                viewModel.resetTripMeter()
                                isHoldingTripReset = false
                            },
                            onPress = {
                                isHoldingTripReset = true
                                tryAwaitRelease()
                                isHoldingTripReset = false
                            }
                        )
                    },
                containerColor = if (isHoldingTripReset) Color.Red else Color.White.copy(alpha = 0.9f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gas_tank),
                    contentDescription = "Reset Trip (Hold)",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}


@Composable
private fun SpeedCard(
    title: String,
    value: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = if (value.toFloat() < 1f) {
                            "0"
                        } else {
                            String.format(Locale.US, "%.1f", value)   // 1 decimal, rounded half‑up
                        },
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}