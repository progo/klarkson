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
                year = analyzeAlbumYear(songs),
                discCount = analyzeDiscCount(songs),
                songs = songs
            )
        }

    return albums
}

/**
 * Take an album's [songs] and try to analyze a release year for it.
 * The vast majority of songs will have same year tagged so no ambiguity there.
 * Some select albums (VA collections, mostly) have a mixture of years across
 * the songs and there we probably won't know the truth, but it has to be at
 * least after the latest song on the album.
 */
fun analyzeAlbumYear(songs: Collection<Song>): Int? {
    val maxYear = songs.maxOfOrNull { it.year ?: -1 }
    return if (maxYear == -1) null else maxYear
}


/**
 * Take an album's [songs] and try to deduce the disc/media count.
 */
fun analyzeDiscCount(songs: Collection<Song>) : Int {
    // TODO add extra measures based on runtime perhaps?
    return songs.maxOf { it.discNumber ?: 1 }
}