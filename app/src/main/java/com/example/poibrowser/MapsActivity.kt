package com.example.poibrowser

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.poibrowser.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var requestHelper: RequestHelper

    private var currentMapMarkers = mutableListOf<Marker>()

    private var isInitialLocationCall = true
    private var isLocationPermissionDenied = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        locationHelper = LocationHelper(applicationContext, this)
        mapFragment.getMapAsync(this)

        requestHelper = RequestHelper(applicationContext, pointOfInterestRequestHandler)
    }

    override fun onResume() {
        super.onResume()
        locationHelper.requestLocationUpdates(handleLocationResult)
    }

    override fun onPause() {
        super.onPause()
        locationHelper.stopLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        Toast.makeText(
            this, " ${marker.title}",
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    private val pointOfInterestRequestHandler =
        RequestHelper.PointOfInterestRequestHandler { poiList: List<PointOfInterest> ->
            removeExpiredMarkers(currentMapMarkers, poiList)
            poiList.forEach { poi ->
                val poiLocation = poi.lat?.let { poi.lon?.let { it1 -> LatLng(it, it1) } }
                poiLocation?.let {
                    MarkerOptions()
                        .position(it)
                        .title(poi.title)
                }?.let {
                    val marker: Marker? = mMap.addMarker(it)
                    marker?.let { it1 -> currentMapMarkers.add(it1) }
                }
            }
        }

    /**
     * Remove expired markers by comparing the locations of current markers and new points of interest
     */
    private fun removeExpiredMarkers(
        previousMarkers: MutableList<Marker>,
        poiList: List<PointOfInterest>
    ) {
        val locations = poiList.map { poi ->
            poi.lat?.let { poi.lon?.let { it1 -> LatLng(it, it1) } }
        }
        previousMarkers.forEach { marker ->
            if (!locations.contains(marker.position)) {
                marker.remove()
            }
        }
    }

    private val handleLocationResult = object : LocationCallback() {
        @SuppressLint("MissingPermission")
        override fun onLocationResult(result: LocationResult) {
            mMap.isMyLocationEnabled = true
            val location = result.locations.first()
            if (isInitialLocationCall) {
                isInitialLocationCall = false
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            location.latitude, location.longitude
                        ), 15f
                    )
                )
            }
            CoroutineScope(Main).launch {
                requestHelper.getPointsOfInterest(location.latitude, location.longitude)
            }
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        checkLocationPermissions()
        if (isLocationPermissionDenied) {
            showMissingPermissionError()
            isLocationPermissionDenied = false
        }
    }

    private fun checkLocationPermissions() {
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 2. If if a permission rationale dialog should be shown
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // 3. Otherwise, request permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )

    }

    private fun showMissingPermissionError() {
        // TODO: Instead of a toast, show a dialog to change the permission via settings
        Toast.makeText(
            this,
            "Location permissions must be allowed in order for the app to work properly. Please allow permissions in the app settings.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationPermissions()
        } else {
            isLocationPermissionDenied = true
        }
    }
}