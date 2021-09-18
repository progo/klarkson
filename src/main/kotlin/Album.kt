package klarksonmainframe

import org.bff.javampd.server.Mpd
import org.bff.javampd.song.MpdSong

data class Album(
    /** album artist */
    val artist: String,
    val album: String,
    val year: Int?,
    val discCount: Int,
    val songs: List<Song>,
    val runtime: Time = songs.sumOf { s -> s.runtime },
)

data class Song(
    val artist: String,
    val album: String,
    val title: String,
    val file: String,
    val albumartist: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val comment: String?,
    val genre: String?,
    val runtime: Time
) {
    companion object {
        fun read(mpd: Mpd, t: MpdSong) : Song {
            return Song(
                artist = t.artistName,
                album = t.albumName,
                title = t.title,
                file = t.file,
                runtime = t.length,
                year = t.year.toIntOrNull(),
                albumartist = mpd.getAlbumArtist(t),
                comment = t.comment,
                genre = t.genre,
                discNumber = t.discNumber.toIntOrNull(),
                trackNumber = t.track
            )
        }
    }
}

/*
Might be that we skip normalized Album/Song model and go for
a linear structure of only Songs?

Can't do that. We need an Album model for canvas.

But we will read Songs from MPD and build albums as they form.
*/


/**
 *  Take a list of MpdSongs from Mpd (assume the [songs] are reasonably ordered
 *  by MPD response) and collect them into Albums. The albums will be incomplete
 *  if songs is incomplete.
 */
fun collectIntoAlbums(mpd: Mpd, songs: Collection<MpdSong>) : List<Album> {

    val albums = songs
        .map { song -> Song.read(mpd, song) }
        .groupBy { s -> Pair(s.albumartist ?: s.artist, s.album) }
        .map { (artistalbum, songs) ->
            val (artist, album) = artistalbum
            Album(
                artist = artist,
                album = album,
                year = songs.maxOfOrNull { it.year ?: 0 },
                discCount = songs.maxOf { it.discNumber ?: 1 },
                songs = songs
            )
        }

    return albums
}