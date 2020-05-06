package it.unibo.yahm.client.sensors

import io.reactivex.rxjava3.core.Observable

class BufferedSubject<T>(private val predicate: (MutableCollection<T>) -> Boolean) {

    private var buffer: MutableCollection<T> = mutableListOf()

    fun buffer(incomingData: Observable<T>): Observable<Collection<T>> {
        return Observable.create { emitter ->
            incomingData.subscribe {
                buffer.add(it)

                if (predicate(buffer)) {
                    emitter.onNext(buffer.toList())
                    buffer.clear()
                }
            }
        }
    }
}
