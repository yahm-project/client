package it.unibo.yahm.client.entities

import kotlin.math.max
import kotlin.math.min

/**
 * Represents the connection leg between nodes
 */
data class Leg(
    val from: Node,
    val to: Node,
    var quality: Int,
    var obstacles: Map<ObstacleType, List<Coordinate>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Leg

        if (max(from.id, to.id) != max(other.from.id, other.to.id)) return false
        if (min(from.id, to.id) != min(other.from.id, other.to.id)) return false

        return true
    }

    override fun hashCode(): Int {
        return (31 * max(from.id, to.id) + min(from.id, to.id)).toInt()
    }
}