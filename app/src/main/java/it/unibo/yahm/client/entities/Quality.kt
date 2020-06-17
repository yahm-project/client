package it.unibo.yahm.client.entities

import android.graphics.Color
import kotlin.random.Random


enum class Quality(val value: Int, val color: Int) {
    VERY_BAD(0, Color.parseColor("#7F0000")),
    BAD(1, Color.parseColor("#D32F2F")),
    OK(2, Color.parseColor("#FF8F00")),
    GOOD(3, Color.parseColor("#FBC02D")),
    PERFECT(4, Color.parseColor("#388E3C"));

    companion object {
        private val map = values().associateBy(Quality::value)
        fun fromValue(value: Int) = map[value]

        fun random() = map[Random.nextInt(0, map.size)] ?: error("out of range")
    }
}
