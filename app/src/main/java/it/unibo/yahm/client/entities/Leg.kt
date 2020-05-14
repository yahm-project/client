package it.unibo.yahm.client.entities

data class Leg(
    val from: Node,
    val to: Node,
    var quality: Double,
    var obstacles: Map<ObstacleType, List<Coordinate>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Leg

        if (from != other.from) return false
        if (to != other.to) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + to.hashCode()
        return result
    }
}