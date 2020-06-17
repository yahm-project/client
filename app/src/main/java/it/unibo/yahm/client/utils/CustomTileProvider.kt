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
            "pk.eyJ1IjoiY3JhZnRvbmkiLCJhIjoiY2tiamF2ZmVkMG5mbTJ4bWI2ZnB3MnhlcCJ9.0uMfR2ze3WOhl_lYKvzrLQ"
    }
}
