package com.example.poibrowser

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.example.poibrowser.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.koushikdutta.ion.Ion

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationHelper: LocationHelper

    private var locationPermissionDenied = false

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
        checkLocationPermissions()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        Toast.makeText(
            this, " ${marker.title}",
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    private fun showMissingPermissionError() {
        Toast.makeText(
            this,
            "Location permissions must be allowed in order for the app to work properly. Please allow permissions in the app settings.",
            Toast.LENGTH_LONG
        ).show()
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
            locationHelper.requestLocationUpdates(initialLocation)
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

    private fun requestMarkers(latitude: Double, longitude: Double) {
        Ion.with(applicationContext)
            .load("https://en.wikipedia.org/w/api.php?action=query")
            .addQuery("generator", "geosearch")
            .addQuery("prop", "coordinates|pageimages|description|info")
            .addQuery("pithumbsize", "400")
            .addQuery("ggsradius", "500")
            .addQuery("ggslimit", "10")
            .addQuery("format", "json")
            .addQuery("ggscoord", "${latitude}|${longitude}")
            .asJsonObject()
            .setCallback { e, result ->
                if (e != null) {
                    Log.e("MapsActivity", "Something went wrong! ${e.message}")
                } else {
                    val queryResult =
                        result.get("query").asJsonObject.get("pages").asJsonObject.entrySet()
                    for ((_, value) in queryResult) {
                        // TODO create objects list from values and don't update existing ones
                        val marker = value.asJsonObject
                        val pageId = marker.get("pageid")
                        val title = marker.get("title")
                        val thumbnail = marker.get("thumbnail")?.asJsonObject?.get("source")
                        val description = marker.get("description")
                        val location = marker.get("coordinates").asJsonArray.first().asJsonObject
                        val coordinates =
                            LatLng(location.get("lat").asDouble, location.get("lon").asDouble)
                        mMap.addMarker(
                            MarkerOptions().position(coordinates).title(title.toString())
                        )
                    }
                }
            }
    }

    private val initialLocation = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.locations.first()
            val coordinates = LatLng(location.latitude, location.longitude)
            requestMarkers(location.latitude, location.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 15F))
        }
    }

    private val handleLocationResult = object : LocationCallback() {
        @SuppressLint("MissingPermission")
        override fun onLocationResult(result: LocationResult) {
            mMap.isMyLocationEnabled = true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (locationPermissionDenied) {
            showMissingPermissionError()
            locationPermissionDenied = false
        }
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
            locationPermissionDenied = true
        }
    }
}