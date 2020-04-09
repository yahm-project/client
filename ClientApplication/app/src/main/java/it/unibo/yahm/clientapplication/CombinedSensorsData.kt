package it.unibo.yahm.clientapplication

data class CombinedSensorsData(val xAcceleration: Float,
                               val yAcceleration: Float,
                               val zAcceleration: Float,
                               val xAngularVelocity: Float,
                               val yAngularVelocity: Float,
                               val zAngularVelocity: Float,
                               val latitude: Double,
                               val longitude: Double,
                               val speed: Float)

class CombinedSensorsDataBuilder {
    private var xAcceleration: Float? = null
    private var yAcceleration: Float? = null
    private var zAcceleration: Float? = null
    private var xAngularVelocity: Float? = null
    private var yAngularVelocity: Float? = null
    private var zAngularVelocity: Float? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var speed: Float? = null

    //function apply is for fluent design
    fun xAcceleration(acceleration: Float) = apply { this.xAcceleration = acceleration }
    fun yAcceleration(acceleration: Float) = apply { this.yAcceleration = acceleration }
    fun zAcceleration(acceleration: Float) = apply { this.zAcceleration = acceleration }
    fun xAngularVelocity(velocity: Float) = apply { this.xAngularVelocity = velocity }
    fun yAngularVelocity(velocity: Float) = apply { this.yAngularVelocity = velocity }
    fun zAngularVelocity(velocity: Float) = apply { this.zAngularVelocity = velocity }
    fun latitude(latitude: Double) = apply { this.latitude = latitude }
    fun longitude(longitude: Double) = apply { this.longitude = longitude }
    fun speed(speed: Float) = apply { this.speed = speed }
    //if some of params value are null the Builder throws an Exception
    fun build() = CombinedSensorsData(xAcceleration!!, yAcceleration!!, zAcceleration!!, xAngularVelocity!!, yAngularVelocity!!, zAngularVelocity!!, latitude!!, longitude!!, speed!!)
}
