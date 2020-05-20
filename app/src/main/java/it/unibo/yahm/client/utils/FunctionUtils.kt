package it.unibo.yahm.client.utils

import kotlin.math.pow
import kotlin.math.sqrt

object FunctionUtils {

    fun <T: Comparable<T>> List<T>.median(): T {
        if (this.isNotEmpty()) {
            return this.sorted()[this.size / 2]
        }
        throw NoSuchElementException()
    }

    fun List<Double>.stdDeviation(): Double {
        if (this.isEmpty()) {
            throw NoSuchElementException()
        }

        return sqrt(this.map { (it - this.average()).pow(2.0) }.average())
    }

}