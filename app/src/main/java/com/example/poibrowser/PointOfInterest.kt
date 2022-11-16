package com.example.poibrowser

import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonArray

data class PointOfInterest(
    val pageId: Int,
    val markerId: Int? = null,
    val title: String,
    val description: String,
    val coordinates: JsonArray? = null,
    val latLng: LatLng,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val thumbnailUrl: String? = null
    // val thumbnail: Bitmap? = null
) {
    override fun toString(): String {
        return "PointOfInterest(pageId=$pageId, title=$title, description=$description," +
                " coordinates=$coordinates, lat=$latitude, lon=$longitude)"
    }
}