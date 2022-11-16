package com.example.poibrowser

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
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
        withContext(IO) {
            Ion.with(mContext)
                .load("https://en.wikipedia.org/w/api.php")
                .addQuery("action", "query")
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
                        pointOfInterestRequestHandler.onPointsOfInterestFetched(emptyList())
                    } else {
                        val queryResult =
                            result.get("query")?.asJsonObject?.get("pages")?.asJsonObject?.entrySet()

                        if (queryResult != null) {
                            pointOfInterestRequestHandler.onPointsOfInterestFetched(
                                fromQueryResultToPointsOfInterest(queryResult)
                            )
                        } else {
                            pointOfInterestRequestHandler.onPointsOfInterestFetched(emptyList())
                        }
                    }
                }
        }
    }

    private fun fromQueryResultToPointsOfInterest(queryResult: Set<Map.Entry<String, JsonElement>>): List<PointOfInterest> {
        return queryResult.map { (_, value) ->
            val wikiPage = Gson().fromJson(value, Page::class.java)
            val coordinates = wikiPage.coordinates.first()

            PointOfInterest(
                pageId = wikiPage.id,
                title = wikiPage.title,
                description = wikiPage.description,
                thumbnailUrl = wikiPage.thumbnail?.url,
                latLng = LatLng(coordinates.latitude, coordinates.longitude),
                latitude = coordinates.latitude,
                longitude = coordinates.longitude
            )
        }
    }

    data class Coordinates(
        @SerializedName("lat") val latitude: Double,
        @SerializedName("lon") val longitude: Double,
    )

    data class Source(
        @SerializedName("source") val url: String,
    )

    data class Page(
        @SerializedName("pageid") val id: Int,
        @SerializedName("title") val title: String,
        @SerializedName("description") val description: String,
        @SerializedName("coordinates") val coordinates: List<Coordinates>,
        @SerializedName("thumbnail") val thumbnail: Source?
    )
}