package com.example.poibrowser

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
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
import com.koushikdutta.ion.Ion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var requestHelper: RequestHelper

    private var currentMapMarkers = mutableMapOf<Marker, PointOfInterest>()

    private var isInitialLocationCall = true
    private var lastClickedMarker: Marker? = null

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

        binding.infoWindow.visibility = View.INVISIBLE
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
        checkLocationPermissions()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        binding.infoWindow.visibility = View.VISIBLE

        if (currentMapMarkers.contains(lastClickedMarker)) {
            lastClickedMarker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        }
        lastClickedMarker = marker
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))

        val wikiPage = currentMapMarkers[marker]
        binding.infoDescription.text = wikiPage?.description
        binding.infoTitle.text = wikiPage?.title
        if (wikiPage != null) {
            Ion.with(binding.infoWindowThumbnail)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .load(wikiPage.thumbnailUrl).withBitmapInfo()
                .setCallback { e, result ->
                    if (e == null) {
                        val bmp = result?.bitmapInfo?.bitmap
                        val imageView = binding.infoWindowThumbnail
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        imageView.setImageBitmap(bmp)
                    }
                }

            binding.infoWindowButton.setOnClickListener {
                openWikiPage(wikiPage.pageId)
            }

        }
        return false
    }

    private fun openWikiPage(pageId: Int) {
        val openURL = Intent(Intent.ACTION_VIEW)
        openURL.data = Uri.parse("https://en.wikipedia.org/w/index.php?curid=${pageId}")
        ContextCompat.startActivity(this, openURL, null)
    }

    private val pointOfInterestRequestHandler =
        RequestHelper.PointOfInterestRequestHandler { poiList: List<PointOfInterest> ->
            currentMapMarkers = removeExpiredMarkers(currentMapMarkers, poiList)
            poiList.forEach { poi ->
                val locations = currentMapMarkers.map { it.value.pageId }
                if (!locations.contains(poi.pageId)) {
                    val marker: Marker? = mMap.addMarker(MarkerOptions().position(poi.latLng))
                    marker?.let { currentMapMarkers[it] = poi }
                }
            }
        }

    /**
     * Remove expired markers by comparing the locations of current markers and new points of interest.
     * If an existing marker's location is not included in the new points of interest, remove it.
     */
    private fun removeExpiredMarkers(
        previousMarkers: MutableMap<Marker, PointOfInterest>,
        poiList: List<PointOfInterest>
    ) : MutableMap<Marker, PointOfInterest> {
        val locations = poiList.map { poi -> poi.latLng }
        previousMarkers.keys.forEach { marker ->
            if (!locations.contains(marker.position)) {
                marker.remove()
            }
        }
        return previousMarkers.filter { m -> locations.contains(m.value.latLng) }.toMutableMap()
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
            mMap.isMyLocationEnabled = true
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

    private fun showMissingPermissionAlert() {
        // TODO: Instead of a toast, show a dialog to change the permission via settings

        AlertDialog.Builder(this)
            .setTitle("Location permission required")
            .setMessage("Location permissions must be allowed in order for the app to work properly. Please allow location permissions in the app settings.")
            .setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            showMissingPermissionAlert()
            return
        }
    }
}