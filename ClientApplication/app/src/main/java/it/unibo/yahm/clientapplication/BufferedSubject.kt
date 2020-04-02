package it.unibo.yahm.clientapplication

import android.util.Log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.ReplaySubject

class BufferedSubject<T>(private val predicate: (MutableCollection<T>) -> Boolean ) {

    private var buffer: MutableCollection<T> = mutableListOf()
    private val bufferedSubject: ReplaySubject<MutableCollection<T>> = ReplaySubject.create()

    fun buffer(incomingData: Observable<T>): Observable<MutableCollection<T>> {
        incomingData.subscribe {
            buffer.add(it)
            if (predicate(buffer)) {
                bufferedSubject.onNext(buffer)
                buffer = mutableListOf()
            }
        }
        return bufferedSubject;
    }


}