package klarksonmainframe.utils

import klarksonmainframe.Album
import klarksonmainframe.toMinutes

/*

Query strings and formats we want to support.

> pink floyd

Search for "pink.+floyd" in artists, albums

> "dark side"

Search for "dark side" verbatim in artists, albums

> \a daft punk

Seach for "daft punk" in artists only

> \a daft punk \b tron

Artists "daft.+punk" and albums "tron"

> \l -10

Albums of runtime 10 minutes or less.

\l 10-35

Albums of runtime between 10-35 minutes.

\l 50

Albums apprx 50 min

*/

const val FLAG_STRING = "\\"
const val ARTIST_FLAG = FLAG_STRING + "a"
const val ALBUM_FLAG = FLAG_STRING + "b"
const val RUNTIME_FLAG = FLAG_STRING + "l"

enum class SearchMode { MATCH_ALL, MATCH_ANY }

interface RuntimeQuery {
    fun matches(a : Album) : Boolean
}

private fun Int.between(min: Double, max: Double) : Boolean = (this >= min) && (this <= max)

class RuntimeApproxQuery(
    private val approxTime : Int
) : RuntimeQuery {
    override fun matches(a: Album): Boolean = a.runtime.toMinutes().between(approxTime * 0.9, approxTime * 1.1)
}

class RuntimeRangeQuery(
    private val timeMin : Int = 0,
    private val timeMax : Int = Integer.MAX_VALUE
) : RuntimeQuery {
    override fun matches(a: Album): Boolean = a.runtime.toMinutes().between(timeMin.toDouble(), timeMax.toDouble())
}

/**
 *  A search statement that's parsed into components.
 *  Strings may be empty.
 */
data class ParsedSearch(
    val artist: String,
    val album: String,
    val runtime: String
)

/**
 * Regexified and ready-to-query searches
 */
data class ParsedSearchQuery(
    val artist: Regex?,
    val album: Regex?,
    val runtime: RuntimeQuery?,
    val matchMode: SearchMode
)


/**
 * Assemble parsing and query building in one...
 */
fun search(searchQuery: String) : ParsedSearchQuery =
    buildQuery(parseSearch(searchQuery))


/**
 *  Build regex matchers and runtime matchers from parsed search input.
 */
fun buildQuery(ps: ParsedSearch) : ParsedSearchQuery {

    var runtimeQ : RuntimeQuery? = null

    if (ps.runtime.isNotBlank()) {
        fun String.isNumba() = toIntOrNull() != null

        // we look for a range of runtimes
        if (ps.runtime.contains('-')) {
            val (min, max) = ps.runtime.split('-')
            if (!min.isNumba() && !max.isNumba())
                runtimeQ = null
            else if (!min.isNumba() && max.isNumba())
                runtimeQ = RuntimeRangeQuery(timeMax = max.toInt())
            else if (min.isNumba() && !max.isNumba())
                runtimeQ = RuntimeRangeQuery(timeMin = min.toInt())
            else
                runtimeQ = RuntimeRangeQuery(timeMin = min.toInt(), timeMax = max.toInt())
        }

        // we look for approx runtimes around one value
        else {
            if (ps.runtime.isNumba()) {
                runtimeQ = RuntimeApproxQuery(approxTime = ps.runtime.toInt())
            }
        }
    }

    /**
     * Make a regex from [s] to loosely build a fuzzy matcher:
     * 'aa bee' turns into a regex that matches 'aa.+bee' somewhere inside the haystack
     */
    fun regexify(s: String): Regex? {
        if (s.isBlank())
            return null

        val delim = " +".toRegex()
        val words = s.split(delim)
        return Regex( "^.*" + words.joinToString(".+") + ".*$",
            RegexOption.IGNORE_CASE
        )
    }

    return ParsedSearchQuery(
        artist = regexify(ps.artist),
        album = regexify(ps.album),
        runtime = runtimeQ,
        matchMode = SearchMode.MATCH_ANY
    )
}


/**
 *
 * Parse a user-inputed search term into components
 */
fun parseSearch(query: String) : ParsedSearch {
    var artist : String = ""
    var album : String = ""
    var runtimeStr = ""
    var ambiguousQuery = false

    query.trim().splitWithDelims(FLAG_STRING).forEach { component ->
        val comp = component.trim()

        if (comp.startsWith(ARTIST_FLAG)) {
            artist = comp.trimString(ARTIST_FLAG)
        }

        else if (comp.startsWith(ALBUM_FLAG)) {
            album = comp.trimString(ALBUM_FLAG)
        }

        else if (comp.startsWith(RUNTIME_FLAG)) {
            runtimeStr = comp.trimString(RUNTIME_FLAG)
        }

        // In this case we have an ill-written query, where we can
        // just guess and come up with a DWIM style logic.
        else {
            artist = comp
            album = comp
            ambiguousQuery = true
        }
    }

    if (ambiguousQuery) {
        // TODO
        // We could message or something.
    }

    return ParsedSearch(artist = artist, album = album, runtime = runtimeStr)
}