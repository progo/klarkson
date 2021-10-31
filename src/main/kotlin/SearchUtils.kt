package klarksonmainframe

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

*/

const val FLAG_STRING = "\\"
const val ARTIST_FLAG = FLAG_STRING + "a"
const val ALBUM_FLAG = FLAG_STRING + "b"

enum class SearchMode { MATCH_ALL, MATCH_ANY }

data class ParsedSearchQuery(
    val artist: Regex?,
    val album: Regex?,
    val runtime: Int?,
    val matchMode: SearchMode
)


fun parseQuery(query: String) : ParsedSearchQuery {
    val q = query.trim()
    var mode = SearchMode.MATCH_ANY
    var artist : String? = null
    var album : String? = null
    var ambiguousQuery = false

    q.splitWithDelims(FLAG_STRING).forEach { component ->
        val comp = component.trim()

        if (comp.startsWith(ARTIST_FLAG)) {
            artist = comp.trimString(ARTIST_FLAG)
        }

        else if (comp.startsWith(ALBUM_FLAG)) {
            album = comp.trimString(ALBUM_FLAG)
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
        runtime=null,
        matchMode=mode
    )
}