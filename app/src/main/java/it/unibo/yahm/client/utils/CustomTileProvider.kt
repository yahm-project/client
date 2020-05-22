package it.unibo.yahm.client.utils

import android.graphics.*
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.UrlTileProvider
import java.io.ByteArrayOutputStream
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


class CanvasTileProvider(private val mTileProvider: TileProvider) : TileProvider {
    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        val data: ByteArray
        val image = newBitmap
        val canvas = Canvas(image)
        val isOk = onDraw(canvas, zoom, x, y)
        data = bitmapToByteArray(image)
        image.recycle()
        return if (isOk) {
            Tile(
                TILE_SIZE,
                TILE_SIZE,
                data
            )
        } else {
            mTileProvider.getTile(x, y, zoom)
        }
    }

    var paint = Paint()
    private fun onDraw(canvas: Canvas, zoom: Int, x: Int, y: Int): Boolean {
        var x = x * 2
        var y = y * 2

        val leftTop = mTileProvider.getTile(x, y, zoom + 1)
        val leftBottom =
            mTileProvider.getTile(x, y + 1, zoom + 1)
        val rightTop =
            mTileProvider.getTile(x + 1, y, zoom + 1)
        val rightBottom =
            mTileProvider.getTile(x + 1, y + 1, zoom + 1)
        if (leftTop == TileProvider.NO_TILE && leftBottom == TileProvider.NO_TILE && rightTop == TileProvider.NO_TILE && rightBottom == TileProvider.NO_TILE) {
            return false
        }
        var bitmap: Bitmap
        if (leftTop != TileProvider.NO_TILE) {
            bitmap = BitmapFactory.decodeByteArray(leftTop.data, 0, leftTop.data.size)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            bitmap.recycle()
        }
        if (leftBottom != TileProvider.NO_TILE) {
            bitmap = BitmapFactory.decodeByteArray(leftBottom.data, 0, leftBottom.data.size)
            canvas.drawBitmap(bitmap, 0f, 256f, paint)
            bitmap.recycle()
        }
        if (rightTop != TileProvider.NO_TILE) {
            bitmap = BitmapFactory.decodeByteArray(rightTop.data, 0, rightTop.data.size)
            canvas.drawBitmap(bitmap, 256f, 0f, paint)
            bitmap.recycle()
        }
        if (rightBottom != TileProvider.NO_TILE) {
            bitmap = BitmapFactory.decodeByteArray(rightBottom.data, 0, rightBottom.data.size)
            canvas.drawBitmap(bitmap, 256f, 256f, paint)
            bitmap.recycle()
        }
        return true
    }

    private val newBitmap: Bitmap
        get() {
            val image = Bitmap.createBitmap(
                TILE_SIZE, TILE_SIZE,
                Bitmap.Config.ARGB_8888
            )
            image.eraseColor(Color.TRANSPARENT)
            return image
        }

    companion object {
        const val TILE_SIZE = 512
        private fun bitmapToByteArray(bm: Bitmap): ByteArray {
            val bos = ByteArrayOutputStream()
            bm.compress(Bitmap.CompressFormat.PNG, 100, bos)
            val data = bos.toByteArray()
            try {
                bos.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return data
        }
    }

}