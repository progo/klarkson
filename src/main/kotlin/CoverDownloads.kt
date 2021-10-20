package klarksonmainframe

import java.io.File
import java.io.FileOutputStream
import java.net.URL

const val COVER_DIRECTORY = "/home/progo/.cache/albumcovers/"
fun albumCoverStorage(album: Album) = File(COVER_DIRECTORY + album.readableHash())

/**
 *
 */
fun getOrDownloadCover(album: Album) : String? {
    val path = albumCoverStorage(album)

    if (path.exists()) {
        return path.absolutePath
    }

    val cover = downloadCover(album)

    // Download might have failed for whatever reason.
    // here [cover] either (== path) or (== null)

    if (cover == null) {
        // Touch an empty file. Zero-sized files are an easy flag and indicator
        // that we don't constantly be connecting to the server.
        path.createNewFile()
    }

    return cover
}



/**
 * Download and store an album cover for [album]. If [directUri] is passed,
 * use this for cover. If not, we ask Last.Fm for covers.
 */
fun downloadCover(album: Album, directUri: Uri? = null) : String? {
    val uri = directUri ?: LastFmClient.getAlbumCoverUri(album.artist, album.album)

    if (uri == null || uri == "") {
        println("Did not get cover for (${album.artist}, ${album.album})")
        return null
    }

    println("Downloading cover for (${album.artist}, ${album.album})")

    val path = albumCoverStorage(album)

    URL(uri).openStream().use { input ->
        FileOutputStream(path).use { output ->
            input.copyTo(output)
        }
    }

    return path.absolutePath
}

/**
 * Copy a local cover image to the normalized cover storage.
 */
fun persistCover(album: Album, cover: File) : String {
    val path = albumCoverStorage(album)
    cover.copyTo(path, overwrite = true)
    return path.absolutePath
}