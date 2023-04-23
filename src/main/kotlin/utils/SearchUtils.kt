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


data class ParsedSearchQuery(
    val artist: Regex?,
    val album: Regex?,
    val runtime: RuntimeQuery?,
    val matchMode: SearchMode
)


fun parseQuery(query: String) : ParsedSearchQuery {
    val q = query.trim()
    var mode = SearchMode.MATCH_ANY
    var artist : String? = null
    var album : String? = null
    var runtimeStr = ""
    var ambiguousQuery = false

    q.splitWithDelims(FLAG_STRING).forEach { component ->
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
            /*
            if (artist != null && album == null)
                album = comp

            if (artist == null && album != null)
                artist = comp
             */

            artist = comp
            album = comp
            ambiguousQuery = true
        }
    }

    if (ambiguousQuery) {
        // We could message or something.
    }

    var runtimeQ : RuntimeQuery? = null

    if (runtimeStr.isNotBlank()) {

        fun String.isNumba() : Boolean {
            // if (isBlank()) return false
            if (toIntOrNull() == null) return false
            return true
        }

        // we look for a range of runtimes
        if (runtimeStr.contains('-')) {
            val (min, max) = runtimeStr.split('-')
//            try {
//                val min = Integer.valueOf(minStr)
//                val max = Integer.valueOf(maxStr)
//            } catch (e : NumberFormatException) { }

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
            if (runtimeStr.isNumba()) {
                runtimeQ = RuntimeApproxQuery(approxTime = runtimeStr.toInt())
            }
        }
    }
    println("[$runtimeStr] => $runtimeQ")

    val delim = " +".toRegex()
    val artistComps = artist?.split(delim) ?: emptyList()
    val albumComps = album?.split(delim) ?: emptyList()

    fun regexify(comps: List<String>) = if (comps.isEmpty()) { null } else {
        Regex( "^.*" + comps.joinToString(".+") + ".*$",
            RegexOption.IGNORE_CASE
        )}

    return ParsedSearchQuery(
        artist=regexify(artistComps),
        album=regexify(albumComps),
        runtime=runtimeQ,
        matchMode=mode
    )
}