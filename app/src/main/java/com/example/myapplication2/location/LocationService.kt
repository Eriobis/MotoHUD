package com.example.myapplication2.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class LocationService(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    fun startLocationUpdates(
        locationRequest: LocationRequest,
        locationCallback: LocationCallback
    ) {
        Log.d("LocationService", "startLocationUpdates called")
        
        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d("LocationService", "Fine location permission: $fineLocationGranted")
        Log.d("LocationService", "Coarse location permission: $coarseLocationGranted")
        
        if (!fineLocationGranted && !coarseLocationGranted) {
            Log.e("LocationService", "No location permissions granted!")
            return
        }
        
        Log.d("LocationService", "Requesting location updates...")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        ).addOnSuccessListener {
            Log.d("LocationService", "Location updates request successful")
        }.addOnFailureListener { exception ->
            Log.e("LocationService", "Location updates request failed", exception)
        }
    }
    
    fun stopLocationUpdates(locationCallback: LocationCallback) {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}