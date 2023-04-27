package klarksonmainframe.utils

// From https://stackoverflow.com/a/23088000
fun Double.format(digits: Int) = "%.${digits}f".format(this)


/**
 * Split a string retaining the delims.
 * Delim[iter] can be a longer string than a character. The delim will be
 * included in front of a piece:
 *
 * `1+2+3` splits into [1 +2 +3] and not into [1+ 2+ 3]
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


/**
 * Trim a prefix from the beginning of the string,
 * and give a usual trimming afterward.
 */
fun String.trimString(extra : String) : String {
    if (startsWith(extra)) {
        return substring(extra.length).trim()
    }
    else
        return trim()
}

/**
 * Calculate a median as an Integer
 */
fun median(nums: Collection<Int>) : Int {
    val ns = nums.sorted()
    if (ns.size % 2 == 0) {
        return (ns[ns.size / 2] + ns[(ns.size - 1) / 2]) / 2
    } else {
        return ns[ns.size / 2]
    }
}

/**
 * Given a date string [s] in arbitrary format, extract a year from there.
 * The current implementation is interested in any 4-digit number in it.
 */
fun extractYear(s: String?) : Int? {
    s ?: return null
    val yearRe = "([1-9][0-9]{3})".toRegex()
    val match = yearRe.find(s) ?: return null
    return Integer.parseInt(match.groupValues.first())
}


/**
 * Given a track number such as "17" or "8/10", try to extract
 * the index from there.
 */
fun extractTrackNumber(s: String?) : Int? {
    s ?: return null
    val match = Regex("^[0-9]+").find(s.trim()) ?: return null
    return Integer.parseInt(match.groupValues.first())
}

val extractDiscNumber = ::extractTrackNumber
