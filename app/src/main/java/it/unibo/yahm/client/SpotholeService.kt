package it.unibo.yahm.client

import io.reactivex.rxjava3.core.Observable
import it.unibo.yahm.client.entities.Evaluations
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SpotholeService {

    @POST("roads/evaluations")
    fun sendEvaluations(@Body evaluations: Evaluations): Observable<Void>

    @GET("roads/evaluations")
    fun loadEvaluations(): Call<Boolean>

}
