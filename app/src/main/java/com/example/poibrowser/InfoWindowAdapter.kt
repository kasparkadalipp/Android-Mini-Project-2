package com.example.poibrowser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class InfoWindowAdapter(val context: Context) : InfoWindowAdapter {
    var infoWindow: View = LayoutInflater.from(context).inflate(R.layout.info_window, null)

    private fun setInfoWindowText(marker: Marker) {
        val wikiPage = Gson().fromJson(marker.snippet, Info::class.java)

        infoWindow.findViewById<TextView>(R.id.info_title).text = marker.title
        infoWindow.findViewById<TextView>(R.id.info_description).text = wikiPage.description
//        infoWindow.findViewById<Button>(R.id.info_window_button).setOnClickListener{
//            val openURL = Intent(Intent.ACTION_VIEW)
//            openURL.data = Uri.parse("https://en.wikipedia.org/w/index.php?curid=${wikiPage.pageId}")
//            startActivity(context, openURL, null)
//        }
//        infoWindow.findViewById<ImageView>(R.id.info_window_thumbnail).setImageBitmap() // TODO
    }

    override fun getInfoWindow(marker: Marker): View {
        setInfoWindowText(marker)
        return infoWindow
    }

    override fun getInfoContents(marker: Marker): View {
        setInfoWindowText(marker)
        return infoWindow
    }

    data class Info(
        @SerializedName("description") val description: String,
        @SerializedName("pageId") val pageId: Int,
    )
}