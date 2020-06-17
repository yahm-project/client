package it.unibo.yahm.client.utils

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.URL


class CustomTileProvider(val width: Int = 512, val height: Int = 512) : UrlTileProvider(width, height) {

    @Synchronized
    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
        return URL("https://api.mapbox.com/styles/v1/mapbox/streets-v11/tiles/$width/$zoom/$x/$y?access_token=$MAPBOX_TOKEN")
    }

    companion object {
        private const val MAPBOX_TOKEN =
            "pk.eyJ1IjoiZ2lhY29tb3RvbnRpbmkiLCJhIjoiY2s5Y3h0d2hxMDNjYjNtcGxmYTA3dnYzMSJ9.EoujETnFYtRxAox-ne97mQ"
    }
}
