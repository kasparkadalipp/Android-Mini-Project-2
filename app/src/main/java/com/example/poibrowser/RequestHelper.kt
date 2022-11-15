package com.example.poibrowser

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.koushikdutta.ion.Ion
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class RequestHelper(
    private val mContext: Context,
    private val pointOfInterestRequestHandler: PointOfInterestRequestHandler
) {

    fun interface PointOfInterestRequestHandler {
        fun onPointsOfInterestFetched(pointsOfInterest: List<PointOfInterest>)
    }

    suspend fun getPointsOfInterest(
        latitude: Double,
        longitude: Double
    ) {
        // TODO: Handle request errors, e.g. no internet connection
        withContext(IO) {
            Ion.with(mContext)
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
                        pointOfInterestRequestHandler.onPointsOfInterestFetched(
                            queryResult.map { (_, value) ->
                                Log.d("RequestHelper", "value: $value")
                                val pageId = value.asJsonObject.get("pageid").asInt
                                val title = value.asJsonObject.get("title").asString
                                val description = value.asJsonObject.get("description").asString
                                //val thumbnail = value.asJsonObject.get("thumbnail").asJsonObject
                                //val thumbnailUrl = thumbnail.get("source").asString
                                val coordinates = value.asJsonObject.get("coordinates").asJsonArray
                                val coordinate = coordinates[0].asJsonObject
                                val lat = coordinate.get("lat").asDouble
                                val lon = coordinate.get("lon").asDouble
                                val latLng = LatLng(lat, lon)
                                val markerOptions = MarkerOptions().position(latLng).title(title)

                                PointOfInterest(
                                    pageId,
                                    title,
                                    description,
                                    coordinates,
                                    coordinate,
                                    lat,
                                    lon
                                )
                            })
                    }
                }
        }
    }
}