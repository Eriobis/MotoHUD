package com.example.myapplication2

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.myapplication2.ui.compose.DashboardScreen
import com.example.myapplication2.ui.theme.MyApplication2Theme

class MainActivity : ComponentActivity() {
    
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private var batteryReceiver: BroadcastReceiver? = null
    
    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        Log.d("Permissions", "Fine location granted: $fineLocationGranted")
        Log.d("Permissions", "Coarse location granted: $coarseLocationGranted")
        
        if (fineLocationGranted || coarseLocationGranted) {
            Log.d("Permissions", "Location permissions granted!")
        } else {
            Log.e("Permissions", "Location permissions denied!")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Setup immersive mode
        setupImmersiveMode()
        
        // Monitor USB power status
        setupPowerMonitoring()
        
        // Request location permissions
        requestLocationPermissions()
        
        setContent {
            MyApplication2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
    
    private fun requestLocationPermissions() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d("Permissions", "Current permissions - Fine: $fineLocationGranted, Coarse: $coarseLocationGranted")
        
        if (!fineLocationGranted && !coarseLocationGranted) {
            Log.d("Permissions", "Requesting location permissions...")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            Log.d("Permissions", "Location permissions already granted")
        }
    }
    
    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        
        // Hide system bars
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    private fun setupPowerMonitoring() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                        val isOnUSBPower = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                        
                        adjustBrightness(isOnUSBPower)
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        
        registerReceiver(batteryReceiver, filter)
    }
    
    private fun adjustBrightness(isOnUSBPower: Boolean) {
        try {
            val layoutParams = window.attributes
            
            if (isOnUSBPower) {
                // Set brightness to 100% when on USB power
                layoutParams.screenBrightness = 1.0f
            } else {
                // Use system brightness when on battery
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            
            window.attributes = layoutParams
        } catch (e: Exception) {
            // Handle any brightness adjustment errors
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-apply immersive mode when resuming
        setupImmersiveMode()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up battery receiver
        batteryReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Receiver might not be registered
            }
        }
        
        // Clear keep screen on flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent accidental exit in automotive environment
        // Do nothing or show exit confirmation if needed
    }
}