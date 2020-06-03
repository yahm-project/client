package it.unibo.yahm.client.entities

import android.graphics.Color
import kotlin.random.Random


enum class Quality(val value: Int, val color: Color) {
    VERY_BAD(0, Color.valueOf(139f,0f,0f)),
    BAD(1, Color.valueOf(255f, 99f, 71f)),
    OK(2, Color.valueOf(255f,255f,0f)),
    GOOD(3, Color.valueOf(128f, 128f, 0f)),
    PERFECT(4, Color.valueOf(124f, 252f,0f));

    companion object {
        private val map = values().associateBy(Quality::value)
        fun fromValue(value: Int) = map[value]

        fun random() = map[Random.nextInt(0, map.size)] ?: error("out of range")
    }
}
