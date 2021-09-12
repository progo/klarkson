package klarksonmainframe

//interface Point {
//    val x: Int
//    val y: Int
//}

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

    fun times(scale: Double) : Point = Point(x * scale, y * scale)
    fun times(scale: Int) : Point = Point(x * scale, y * scale)
    operator fun minus(subtract: Point): Point = Point(x - subtract.x, y - subtract.y)
    operator fun minus(subtract: Double): Point = Point(x - subtract, y - subtract)
    operator fun minus(subtract: Int): Point = Point(x - subtract, y - subtract)
    override operator fun compareTo(p: Point): Int = compareXY(this.x, this.y, p.x, p.y)
    fun min(p: Point) = if (this < p) this else p
    fun max(p: Point) = if (this > p) this else p
}

/**
 * Virtual 2D Point
 */
//data class VPoint(
//    override val x: Int,
//    override val y: Int
//) : Point {
//    constructor(x: Double, y: Double) : this(x.toInt(), y.toInt())
//
//    fun times(scale: Double) : VPoint = VPoint(x * scale, y * scale)
//    fun times(scale: Int) : VPoint = VPoint(x * scale, y * scale)
//
//    operator fun compareTo(p: VPoint): Int {
//        if (this.x < p.x) {
//            return -1
//        }
//        else if (this.x == p.x) {
//            return this.y - p.y
//        }
//        else {
//            return 1
//        }
//    }
//
//    fun min(p: VPoint) = if (this < p) this else p
//    fun max(p: VPoint) = if (this > p) this else p
//}
