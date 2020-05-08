package it.unibo.yahm.client.entities

data class Evaluations(
    val coordinates: List<Coordinate>,
    val timestamps: List<Long>,
    val radiuses: List<Double>,
    val qualities: List<Quality>,
    val obstacle: List<Obstacle>
)
