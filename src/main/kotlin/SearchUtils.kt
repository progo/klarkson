package klarksonmainframe

/*

Query strings and formats we want to support.

> pink floyd

Search for "pink floyd" in artists, albums

> \a daft punk

Seach for "daft punk" in artists only


*/

enum class SearchMode { MATCH_ALL, MATCH_ANY }

data class ParsedSearchQuery(
    val artist: String?,
    val album: String?,
    val runtime: Int?,
    val matchMode: SearchMode
)


fun parseQuery(query: String) : ParsedSearchQuery {
    var q = query.trim()
    var mode = SearchMode.MATCH_ANY
    var artist : String? = null
    var album : String? = null

    if (q.startsWith("\\a")) {
        artist = q.substring(2).trim()
    }
    else {
        artist = q
        album = q
    }


    return ParsedSearchQuery(
        artist=artist,
        album=album,
        runtime=null,
        matchMode=mode
    )
}