package com.example.poibrowser

import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class PointOfInterest(
    val pageId: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val coordinates: JsonArray? = null,
    val coordinate: JsonObject? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    // val thumbnailUrl: String? = null
    // val thumbnail: Bitmap? = null
) {
    override fun toString(): String {
        return "PointOfInterest(pageId=$pageId, title=$title, description=$description," +
                " coordinates=$coordinates, coordinate=$coordinate, lat=$lat, lon=$lon)"
    }
}