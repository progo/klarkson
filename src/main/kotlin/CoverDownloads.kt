package klarksonmainframe

import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.imageio.ImageIO

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

    val cover = downloadCoverViaLastFM(album)

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
 * Download cover from using Last.FM API.
 * Alternative Artist/Album search terms can be supplied.
 */
fun downloadCoverViaLastFM(
    album: Album,
    altArtist: String? = null,
    altAlbum: String? = null
) : String? {
    val artistName = altArtist ?: album.artist
    val albumName = altAlbum ?: album.album

    /** last.fm introduced stricter reqs  **/
    if (artistName == "" || albumName == "") {
        return null
    }

    val uri = LastFmClient.getAlbumCoverUri(artistName, albumName)
    return downloadCoverDirect(album, uri)
}

/**
 * Attempt to download the image from given Uri.
 **/
fun downloadCoverDirect(album: Album, uri: Uri? = null) : String? {
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

fun persistCover(album: Album, cover: BufferedImage) : String {
    val path = albumCoverStorage(album)
    ImageIO.write(cover, "png", path)
    return path.absolutePath
}