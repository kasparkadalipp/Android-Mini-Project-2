package com.example.poibrowser

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class MyInfoWindowAdapter(mContext: Context) : GoogleMap.InfoWindowAdapter {
    var mWindow: View = LayoutInflater.from(mContext).inflate(R.layout.info_window, null)

    private fun setInfoWindowText(marker: Marker) {
        val title = marker.title
        val tvTitle = mWindow.findViewById<TextView>(R.id.info_title)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = title
        }
    }

    override fun getInfoWindow(p0: Marker): View {
        setInfoWindowText(p0)
        return mWindow
    }

    override fun getInfoContents(p0: Marker): View {
        setInfoWindowText(p0)
        return mWindow
    }
}