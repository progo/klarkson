package klarksonmainframe

import kotlin.math.pow
import kotlin.math.sqrt

fun compareXY (x1 : Int, y1 : Int, x2 : Int, y2 : Int) : Int {
    if (x1 < x2) {
        return -1
    }
    else if (x1 == x2) {
        return y1 - y2
    }
    else {
        return 1
    }
}

/**
 * Physical 2D point
 */
data class Point( val x: Int, val y: Int )  : Comparable<Point> {
    constructor(x: Double, y: Double) : this(x.toInt(), y.toInt())
    constructor(p: java.awt.Point) : this(p.x, p.y)

    fun times(scale: Double) : Point = Point(x * scale, y * scale)
    fun times(scale: Int) : Point = Point(x * scale, y * scale)
    operator fun minus(subtract: Point): Point = Point(x - subtract.x, y - subtract.y)
    operator fun minus(subtract: Double): Point = Point(x - subtract, y - subtract)
    operator fun minus(subtract: Int): Point = Point(x - subtract, y - subtract)
    override operator fun compareTo(other: Point): Int = compareXY(this.x, this.y, other.x, other.y)
    fun min(p: Point) = if (this < p) this else p
    fun max(p: Point) = if (this > p) this else p

    fun distance(x: Int, y: Int) : Double =
        sqrt((this.x - x.toDouble()).pow(2.0) + (this.y - y.toDouble()).pow(2.0))
    fun distance(p: Point) = this.distance(p.x, p.y)
}
