package com.example.poibrowser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationHelper(private val mContext: Context, private val mapsActivity: MapsActivity) {

    // !NB Don't forget to add 'play-services-location' gradle dependency!

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, // Optional
        5000
    ).build()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private var registeredCallbacks: ArrayList<LocationCallback> = arrayListOf()

    fun requestLocationUpdates(callback: LocationCallback) {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(mContext)
        val checkLocationSettingsTask: Task<LocationSettingsResponse> =
            client.checkLocationSettings(builder.build())

        checkLocationSettingsTask.addOnSuccessListener {
            // All location settings are satisfied. The client can initialize location requests here
            startLocationUpdates(callback)
        }

        checkLocationSettingsTask.addOnFailureListener { exception ->
            stopLocationUpdates()
            if (exception is ApiException) {
                // The device location needs to be enabled, therefore show the user a dialog to enable location
                ResolvableApiException(exception.status).startResolutionForResult(
                    mapsActivity,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(callback: LocationCallback) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
        registeredCallbacks.add(callback)

    }

    fun stopLocationUpdates() {
        registeredCallbacks.forEach {
            LocationServices.getFusedLocationProviderClient(mContext)
                .removeLocationUpdates(it)
        }
        registeredCallbacks.clear()
    }

}