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
    
    // Permission launcher for all permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val bluetoothGranted = permissions[Manifest.permission.BLUETOOTH] ?: false
        val bluetoothAdminGranted = permissions[Manifest.permission.BLUETOOTH_ADMIN] ?: false
        val bluetoothConnectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        val bluetoothScanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
        
        Log.d("Permissions", "=== PERMISSION RESULTS ===")
        Log.d("Permissions", "Fine location granted: $fineLocationGranted")
        Log.d("Permissions", "Coarse location granted: $coarseLocationGranted")
        Log.d("Permissions", "Bluetooth granted: $bluetoothGranted")
        Log.d("Permissions", "Bluetooth Admin granted: $bluetoothAdminGranted")
        Log.d("Permissions", "Bluetooth Connect granted: $bluetoothConnectGranted")
        Log.d("Permissions", "Bluetooth Scan granted: $bluetoothScanGranted")
        
        val locationOk = fineLocationGranted || coarseLocationGranted
        val bleOk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            bluetoothConnectGranted && bluetoothScanGranted
        } else {
            bluetoothGranted && bluetoothAdminGranted
        }
        
        if (locationOk) {
            Log.d("Permissions", "✅ Location permissions granted!")
        } else {
            Log.e("Permissions", "❌ Location permissions denied!")
        }
        
        if (bleOk) {
            Log.d("Permissions", "✅ BLE permissions granted!")
        } else {
            Log.e("Permissions", "❌ BLE permissions denied!")
        }
        
        if (!locationOk || !bleOk) {
            Log.w("Permissions", "⚠️ Some permissions denied. App functionality will be limited.")
            Log.w("Permissions", "💡 Please go to Settings > Apps > ${packageManager.getApplicationLabel(applicationInfo)} > Permissions")
            Log.w("Permissions", "💡 Enable Location and Nearby devices (Bluetooth) permissions")
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
        
        // Request all permissions
        requestAllPermissions()
        
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
    
    private fun requestAllPermissions() {
        Log.d("Permissions", "=== CHECKING PERMISSIONS ===")
        Log.d("Permissions", "Android SDK: ${android.os.Build.VERSION.SDK_INT}")
        
        val permissionsToRequest = mutableListOf<String>()
        
        // Check location permissions
        val fineLocationStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val fineLocationGranted = fineLocationStatus == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = coarseLocationStatus == PackageManager.PERMISSION_GRANTED
        
        Log.d("Permissions", "Fine location status: $fineLocationStatus (granted: $fineLocationGranted)")
        Log.d("Permissions", "Coarse location status: $coarseLocationStatus (granted: $coarseLocationGranted)")
        
        if (!fineLocationGranted) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!coarseLocationGranted) permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        
        // Check BLE permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ - use new BLE permissions
            val connectStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            val scanStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            val bluetoothConnectGranted = connectStatus == PackageManager.PERMISSION_GRANTED
            val bluetoothScanGranted = scanStatus == PackageManager.PERMISSION_GRANTED
            
            Log.d("Permissions", "BLE Connect status: $connectStatus (granted: $bluetoothConnectGranted)")
            Log.d("Permissions", "BLE Scan status: $scanStatus (granted: $bluetoothScanGranted)")
            
            if (!bluetoothConnectGranted) permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (!bluetoothScanGranted) permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            // Android 11 and below - use legacy BLE permissions
            val bluetoothStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
            val bluetoothAdminStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
            val bluetoothGranted = bluetoothStatus == PackageManager.PERMISSION_GRANTED
            val bluetoothAdminGranted = bluetoothAdminStatus == PackageManager.PERMISSION_GRANTED
            
            Log.d("Permissions", "Bluetooth status: $bluetoothStatus (granted: $bluetoothGranted)")
            Log.d("Permissions", "Bluetooth Admin status: $bluetoothAdminStatus (granted: $bluetoothAdminGranted)")
            
            if (!bluetoothGranted) permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            if (!bluetoothAdminGranted) permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        
        Log.d("Permissions", "Permissions to request: ${permissionsToRequest.size}")
        
        if (permissionsToRequest.isNotEmpty()) {
            Log.d("Permissions", "Requesting: ${permissionsToRequest.joinToString()}")
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("Permissions", "✅ All permissions already granted - starting services")
            // Force a check after a delay to see if services start properly
            window.decorView.postDelayed({
                checkServicesStatus()
            }, 1000)
        }
    }
    
    private fun checkServicesStatus() {
        Log.d("Permissions", "=== CHECKING SERVICES STATUS ===")
        // This will help debug if services are working even when permissions look good
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