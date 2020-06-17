package it.unibo.yahm.client.services

import android.content.Context
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import it.unibo.yahm.BuildConfig
import it.unibo.yahm.R
import it.unibo.yahm.client.SpotholeService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitService(context: Context) {

    val spotholeService: SpotholeService

    init {
        val baseUrl = if (BuildConfig.DEBUG) {
            context.resources.getString(R.string.spothole_service_development_baseurl)
        } else {
            context.resources.getString(R.string.spothole_service_production_baseurl)
        }

        val retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()

        spotholeService = retrofit.create(SpotholeService::class.java)
    }

}
