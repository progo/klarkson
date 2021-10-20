package klarksonmainframe

import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL

const val LAST_FM_API_ENDPOINT = "https://ws.audioscrobbler.com/2.0/"

// enum class LastFmCoverSize { small, medium, large, extralarge, mega }
typealias Uri = String


object LastFmClient {
    private var infoCountSucc = 0
    private var infoCountFail = 0
    private var coverFetchSucc = 0
    private var coverFetchFail = 0

    fun getStatistics() : String {
        return "Album info acquisitions $infoCountSucc, failures $infoCountFail.\n" +
                "Covers fetched/failed: $coverFetchSucc/$coverFetchFail."
    }

    private fun getAlbumInfo(artist: String, album: String) : String? {
        val enc = { x : String -> java.net.URLEncoder.encode(x, "UTF-8") }
        val uri = LAST_FM_API_ENDPOINT +
                "?method=album.getinfo" +
                "&api_key=${LAST_FM_API_KEY}" +
                "&artist=" + enc(artist) +
                "&album=" + enc(album) +
                "&format=json"

        val response = StringBuffer()

        val conn : HttpURLConnection = URL(uri).openConnection() as HttpURLConnection
        try {
            with(conn) {
                requestMethod = "GET"
                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        response.append(line)
                    }
                }
            }
        } catch (e : java.io.FileNotFoundException) {
            println("Got ${conn.responseCode} on ($artist, $album).")
            infoCountFail++
            return null
        }

        infoCountSucc++
        return response.toString()
    }

    private fun getAlbumCoverUris(albumResponse: String) : Map<String, Uri> {
        val j = Json.parseToJsonElement(albumResponse)
        val images = j.jsonObject["album"]?.jsonObject?.get("image")

        if (images == null) {
            coverFetchFail++
            return emptyMap()
        }

        images as JsonArray
        val covermap = images
            .filter { i: JsonElement ->
                i as JsonObject
                val uri = i["#text"] as JsonPrimitive
                uri.content != ""
            }
            .associate { i: JsonElement ->
                i as JsonObject
                val szClass = i["size"] as JsonPrimitive
                val uri = i["#text"] as JsonPrimitive
                szClass.content to uri.content
            }

        if (covermap.isEmpty()) {
            coverFetchFail++
        }
        else {
            coverFetchSucc++
        }

        return covermap
    }

    fun getAlbumCoverUri(artist: String, album: String) : Uri? {
        val albumInfo = getAlbumInfo(artist, album) ?: return null
        val candidates = getAlbumCoverUris(albumInfo)

        // println("$artist, $album\n\n$candidates")

        // Right now (Oct 2021) it seems that the biggest file last.fm is
        // serving is 300x300, which is fine by us.
        return when {
            "" in candidates -> candidates[""]
            "mega" in candidates -> candidates["mega"]
            "extralarge" in candidates -> candidates["extralarge"]
            else -> candidates["small"]
        }
    }
}
