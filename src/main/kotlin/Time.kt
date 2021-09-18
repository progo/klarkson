package klarksonmainframe

import kotlin.math.roundToInt

/**
 * Represents a time duration in seconds.
 */
typealias Time = Int

fun Time.toMinutes() : Int = (this / 60.0).roundToInt()

fun Time.toHuman() : String {
    val hours : Int = this / (60 * 60)
    val minutes : Int = this / 60 - (hours * 60)
    val seconds : Int = this % 60
    return when (hours == 0) {
        true  -> String.format("%d:%02d", minutes, seconds)
        false -> String.format("%dh %02dmin", hours, minutes)
    }
}
