package it.unibo.yahm.client.entities

import kotlin.random.Random

enum class Quality(val value: Int) {
    QUALITY0(0),
    QUALITY1(1),
    QUALITY2(2),
    QUALITY3(3),
    QUALITY4(4);

    companion object {
        private val map = values().associateBy(Quality::value)
        fun fromValue(value: Int) = map[value]

        fun random() = map[Random.nextInt(0, map.size)] ?: error("out of range")
    }
}
