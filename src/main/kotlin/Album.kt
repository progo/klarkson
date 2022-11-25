package klarksonmainframe

import org.bff.javampd.song.MpdSong
import org.jetbrains.exposed.sql.ResultRow
import java.security.MessageDigest

data class Album(
    /** album artist */
    val artist: String,
    val album: String,
    val year: Int?,
    val discCount: Int,
    val songs: List<Song>,
    val runtime: Time = songs.sumOf { s -> s.runtime },
) {
    companion object {
        fun make(artist: String, album: String, songs: Collection<Song>) : Album {
            return Album(
                artist = artist,
                album = album,
                year = analyzeAlbumYear(songs),
                discCount = analyzeDiscCount(songs),
                songs = songs.toList()
            )
        }

        fun make(songs: Collection<Song>) =
            make(songs.first().albumArtist ?: songs.first().artist, songs.first().album, songs)

        fun make(r : ResultRow, ss : Iterable<Song>) : Album {
            return Album(
                artist = r[DBAlbum.artist],
                album = r[DBAlbum.album],
                year = r[DBAlbum.year],
                discCount = r[DBAlbum.discCount] ?: 1,
                runtime = r[DBAlbum.runtime],
                songs = ss.toList()
            )
        }
    }

    fun readableHash() : String {
        val msg = "$artist\n$album".lowercase().toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val dig = md.digest(msg)

        val hrdigest = Regex("[^a-z-]").replace("$artist-$album".lowercase(), "")
        val hexdigest = dig.fold("") { str, it -> str + "%02x".format(it) }

        return "$hexdigest-$hrdigest"
    }

    fun createCover() : AlbumCover {
        return AlbumCover(this)
    }
}

data class Song(
    val artist: String,
    val album: String,
    val title: String,
    val file: String,
    val albumArtist: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val comment: String?,
    val genre: String?,
    val runtime: Time
) {
    companion object {
        fun make(r : ResultRow, albumName : String) : Song {
            return Song(
                artist = r[DBTrack.artist],
                album = albumName,
                title = r[DBTrack.title],
                file = r[DBTrack.file],
                runtime = r[DBTrack.runtime],
                year = r[DBTrack.year],
                albumArtist = r[DBTrack.albumArtist],//TODO can be derived via .albumId
                comment = r[DBTrack.comments],
                genre = r[DBTrack.genre],
                discNumber = r[DBTrack.discNumber],
                trackNumber = r[DBTrack.trackNumber]
            )
        }

        fun make(t : MpdSong) : Song {
            return Song(
                artist = t.artistName,
                album = t.albumName,
                title = t.title,
                file = t.file,
                runtime = t.length,
                year = t.year.toIntOrNull(),
                albumArtist = MpdServer.getAlbumArtist(t),
                comment = t.comment,
                genre = t.genre,
                discNumber = t.discNumber.toIntOrNull(),
                trackNumber = t.track
            )
        }
    }
}

val SongSeparator = Song("", "", "", "", "", 0, 0, 0, "", "", 0)

/*
Might be that we skip normalized Album/Song model and go for
a linear structure of only Songs?

Can't do that. We need an Album model for canvas.

But we will read Songs from MPD and build albums as they form.
*/

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