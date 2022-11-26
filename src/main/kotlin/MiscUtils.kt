package klarksonmainframe

// From https://stackoverflow.com/a/23088000
fun Double.format(digits: Int) = "%.${digits}f".format(this)


/**
 * Split a string retaining the delims
 */
fun String.splitWithDelims(delim : String) : List<String> {
    if (isBlank())
        return listOf()

    val pos = indexOf(delim, 1)
    if (pos < 0) {
        return listOf(this)
    }

    return listOf(substring(0, pos)) + substring(pos).splitWithDelims(delim)
}



fun String.trimString(extra : String) : String {
    if (startsWith(extra)) {
        return substring(extra.length).trim()
    }
    else
        return trim()
}

/**
 * Calculate a median
 */
fun median(nums: Collection<Int>) : Int {
    val ns = nums.sorted()
    if (ns.size % 2 == 0) {
        return (ns[ns.size / 2] + ns[(ns.size - 1) / 2]) / 2
    } else {
        return ns[ns.size / 2]
    }
}