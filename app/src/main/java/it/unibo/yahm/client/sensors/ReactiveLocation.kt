package it.unibo.yahm.client.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject


class ReactiveLocation(
    private val context: Context, private val minDistance: Float = 0.0f,
    private val minTime: Long = 100
) {

    private var locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var publishSubject: PublishSubject<Location>? = null
    private var gpsListener: LocationListener? = null
    private var networkListener: LocationListener? = null

    @Synchronized
    fun observe(): Observable<Location> {
        if (publishSubject == null) {
            publishSubject = createObserver()
        }
        return publishSubject!!
    }

    private fun createObserver(): PublishSubject<Location> {
        val subject = PublishSubject.create<Location>()

        val listenerCreator = {
            object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    if (location != null) {
                        subject.onNext(location)
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    // Nope
                }

                override fun onProviderEnabled(provider: String?) {
                    // Nope
                }

                override fun onProviderDisabled(provider: String?) {
                    // Nope
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw IllegalAccessError()
        }

        gpsListener = listenerCreator()
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            minTime,
            minDistance,
            gpsListener!!
        )
        networkListener = listenerCreator()
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            minTime,
            minDistance,
            networkListener!!
        )

        return subject
    }

    @Synchronized
    fun dispose() {
        if (gpsListener != null) {
            locationManager.removeUpdates(gpsListener!!)
            gpsListener = null
        }
        if (networkListener != null) {
            locationManager.removeUpdates(networkListener!!)
            networkListener = null
        }
        if (publishSubject != null) {
            publishSubject!!.onComplete()
            publishSubject = null
        }
    }

}
