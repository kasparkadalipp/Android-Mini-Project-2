package com.example.poibrowser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationHelper(private val mContext: Context) {

    // !NB Don't forget to add 'play-services-location' gradle dependency!

    private val locationRequest  = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, // Optional
        5000
    ).build()

    var registeredCallbacks: ArrayList<LocationCallback> = arrayListOf()

    fun requestLocationUpdates( callback: LocationCallback) {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(mContext)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // All location settings are satisfied. The client can initialize location requests here.
            startLocationUpdates(callback)
        }

        task.addOnFailureListener { exception ->
            // lacking permissions //TODO
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(callback: LocationCallback) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
        fusedLocationClient.requestLocationUpdates(locationRequest,
            callback,
            Looper.getMainLooper())
        registeredCallbacks.add(callback)

    }

    fun stopLocationUpdates(){
        registeredCallbacks.forEach {
            LocationServices.getFusedLocationProviderClient(mContext)
                .removeLocationUpdates(it)
        }
        registeredCallbacks.clear()
    }

}