package it.unibo.yahm.client.utils

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.MalformedURLException
import java.net.URL
import java.util.*


class CustomTileProvider(val width:Int = 512, val height: Int = 512) :
    UrlTileProvider(width, height) {

    @Synchronized
    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
        val s: String =
            java.lang.String.format(Locale.US, MAP_URL, width, zoom, x, y)
        var url: URL? = null
        try {
            url = URL(s)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return url
    }

    companion object {
        private const val MAPBOX_TOKEN = "pk.eyJ1IjoiZ2lhY29tb3RvbnRpbmkiLCJhIjoiY2s5Y3h0d2hxMDNjYjNtcGxmYTA3dnYzMSJ9.EoujETnFYtRxAox-ne97mQ"
        private const val MAP_URL= "https://api.mapbox.com/styles/v1/mapbox/streets-v11/tiles/%d/%d/%d/%d?access_token=$MAPBOX_TOKEN"
    }
}
