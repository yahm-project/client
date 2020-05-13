package it.unibo.yahm.client.utils

object FunctionUtils {

    fun <T: Comparable<T>> List<T>.median(): T {
        if (this.isNotEmpty()) {
            return this.sorted()[this.size / 2]
        }
        throw NoSuchElementException()
    }

}