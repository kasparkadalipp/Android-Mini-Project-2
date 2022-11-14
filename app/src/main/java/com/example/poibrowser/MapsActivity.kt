package com.example.poibrowser

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.example.poibrowser.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.MarkerOptions
import com.koushikdutta.ion.Ion

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationHelper: LocationHelper
    private var initialLaunch = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        locationHelper = LocationHelper(applicationContext)
        if (initialLaunch) locationHelper.requestLocationUpdates(initialLocation)
        mapFragment.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        locationHelper.requestLocationUpdates(myCallBackImplementation)
    }

    override fun onPause() {
        super.onPause()
        locationHelper.stopLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
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
                    val queryResult = result.get("query").asJsonObject.get("pages").asJsonObject.entrySet()
                    for ((_, value) in queryResult) {
                        // TODO create objects list from values and don't update existing ones
                        val marker = value.asJsonObject
                        val pageId = marker.get("pageid")
                        val title = marker.get("title")
                        val thumbnail = marker.get("thumbnail")?.asJsonObject?.get("source")
                        val description = marker.get("description")
                        val location = marker.get("coordinates").asJsonArray.first().asJsonObject
                        val coordinates = LatLng(location.get("lat").asDouble, location.get("lon").asDouble)
                        mMap.addMarker(MarkerOptions().position(coordinates).title(title.toString())) // TODO override on click
                    }
                }
            }
    }

    val initialLocation = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.locations.first()
            val coordinates = LatLng(location.latitude, location.longitude)
            requestMarkers(location.latitude, location.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 15F))
        }
    }

    val myCallBackImplementation = object : LocationCallback() {
        @SuppressLint("MissingPermission")
        override fun onLocationResult(result: LocationResult) {
            mMap.isMyLocationEnabled = true
        }
    }
}