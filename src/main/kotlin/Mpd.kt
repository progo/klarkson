package klarksonmainframe

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.ReceiveChannel
import org.bff.javampd.server.MPD
import org.bff.javampd.song.MPDSong
import org.bff.javampd.song.SongSearcher
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.newSingleThreadContext
import mu.KotlinLogging
import org.bff.javampd.playlist.MPDPlaylistSong
import java.util.TreeSet

private val logger = KotlinLogging.logger {}


/**
 *  Make a producer that streams Album objects as they form from the search.
 *  Outsources the work to its own thread.
 *
 *  When [query] is passed, MPD is asked to search (search type = ANY) by
 *  the keyword.
 *
 *  We stop collecting albums if the album count reaches the [limit].
 */
fun produceAlbums(
    source: TrackSource,
    query: String? = null,
    limit : Int = 100)
: ReceiveChannel<Album> = MainScope().produce(newSingleThreadContext("mpdworker")) {
    val mpdsongs = source.searchSongs(query ?: "")
    var count = limit

    class MPDSongComparator : Comparator<MPDSong> {
        override fun compare(p0: MPDSong, p1: MPDSong): Int {
            // Ordering by:
            // 1. Album
            // 2. Album artist
            // 3. Disc number
            // 4. Track number
            // -1 : p0 < p1
            // +1 : p0 > p1
            // 0  : else
            if (p0.albumName < p1.albumName)
                return -1
            else if (p0.albumName > p1.albumName)
                return 1
            if (p0.albumArtist < p1.albumArtist)
                return -1
            else if (p0.albumArtist > p1.albumArtist)
                return 1
            if (p0.discNumber < p1.discNumber)
                return -1
            else if (p0.discNumber > p1.discNumber)
                return 1
            if (p0.track < p1.track)
                return -1
            else if (p0.track > p1.track)
                return 1
            return p0.title.compareTo(p1.title)
        }
    }

    // queried albums
    val albumset = TreeSet<String>()
    // a full covering of albums that cover the original set of [mpdsongs]
    val songset = TreeSet<MPDSong>(MPDSongComparator())
    for (song in mpdsongs) {
        if (song.albumName in albumset)
            continue
        songset.addAll(source.searchSongs(song.albumName, SongSearcher.ScopeType.ALBUM))
        albumset.add(song.albumName)
    }

    // Now we have a songset that should cover all tracks of [mpdsongs] in full albums
    // logger.debug { songset }
    for (s in songset) {
        println("${s.track}: ${s.artistName} - ${s.albumName} - ${s.title}")
    }

    // Low level work here to make grouping efficiently.
    var songs : MutableList<Song> = ArrayList()
    for (mpdsong in songset) {
        val song = Song.make(mpdsong)

        // New album begins here, send the old one away for processing
        if (albumOf(song) != albumOf(songs)) {
            if (songs.isNotEmpty()) {
                logger.debug { "An album collected, sending... ->" }
                send(Album.make(songs))

                if (--count == 0) {
                    logger.debug { "We hit the limit that we can take!" }
                    return@produce
                }
            }

            songs = ArrayList()
        }

        songs.add(song)
    }

    // Send the last one.
    if (songs.isNotEmpty()) {
        logger.debug { "The last album collected, sending... ->" }
        send(Album.make(songs))
    }
}


object MpdServer {
    private val mpd = MPD.builder().build()
    private val mpdtracks = MPDTrackSource(mpd)

    fun produceAlbums(query: String? = null, limit: Int = 100) =
        produceAlbums(source = mpdtracks, query = query, limit = limit)

    /**
     *
     */
    fun checkFile(song : Song) : FileStatus {
        val results = mpd.songSearcher.search(SongSearcher.ScopeType.FILENAME, song.file)

        if (results.isEmpty())  {
            return FileStatus.MISSING
        }

        val mpdsong = results.first()
        val song2 = Song.make(mpdsong)
        if (song2 != song)
            return FileStatus.TAGS

        return FileStatus.OK
    }

    /**
     * Add given Songs to MPD playlist, and start playing them, if [play].
     */
    fun addTracks(tracks : Collection<Song>, play : Boolean = false, quiet : Boolean = false) {
        val playlist = mpd.playlist
        val position = playlist.songList.size
        tracks.forEach { mpd.playlist.addSong(it.file) }

        // MpdSong.position is a zero-based index so if we want to play the
        // just-added content we find the MpdSong that has the [position].

        if (play) {
            val s : MPDPlaylistSong = playlist.songList.first { it.position == position }
            mpd.player.playSong(s)
        }

        if (!quiet) {
            val msg = "${tracks.count()} tracks added!"
            showMessage(msg, timeMillis = 2000)
        }
    }

    fun addAlbums(albums: Iterable<AlbumCover>, play : Boolean = false) {
        val traxx = albums.flatMap { ac -> ac.album.songs }
        addTracks(traxx, play=play)
    }
}

enum class FileStatus {
    OK,
    MISSING,
    TAGS
}
