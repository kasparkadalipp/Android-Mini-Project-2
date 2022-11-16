package com.example.poibrowser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker

class InfoWindowAdapter(context: Context) : InfoWindowAdapter {
    var infoWindow: View = LayoutInflater.from(context).inflate(R.layout.info_window, null)

    private fun setInfoWindowText(marker: Marker) {
        infoWindow.findViewById<TextView>(R.id.info_title).text = marker.title
    }

    override fun getInfoWindow(marker: Marker): View {
        setInfoWindowText(marker)
        return infoWindow
    }

    override fun getInfoContents(marker: Marker): View {
        setInfoWindowText(marker)
        return infoWindow
    }
}