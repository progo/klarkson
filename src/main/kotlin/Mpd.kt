package klarksonmainframe

import klarksonmainframe.mpd.stripAlbumArtistTag
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.ReceiveChannel
import org.bff.javampd.server.MPD
import org.bff.javampd.song.MPDSong
import org.bff.javampd.song.SongSearcher
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.newSingleThreadContext
import mu.KotlinLogging
import org.bff.javampd.playlist.MPDPlaylistSong

private val logger = KotlinLogging.logger {}


/**
 *  Make a producer that streams Album objects as they form from the search.
 *  Outsources the work to its own thread.
 *
 *  When [query] is passed, MPD is asked to search (search type = ANY) by
 *  the keyword.
 *
 *  We stop collecting albums if the count reaches the [limit].
 */
fun produceAlbums(
    source: TrackSource,
    query: String? = null,
    limit : Int = 100)
: ReceiveChannel<Album> = MainScope().produce(newSingleThreadContext("mpdworker")) {
    val mpdsongs = source.searchSongs(query ?: "")

    logger.debug { "MPD has been queried." }
    var count = limit

    // Low level work here to make grouping efficiently.
    var songs : MutableList<Song> = ArrayList()
    for (mpdsong in mpdsongs) {
        if (AlbumStore.knowFile(mpdsong.file))
            continue
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

    /**
     * Get an Album artist information for given [song].
     */
    fun getAlbumArtist(song: MPDSong) : String {
        val resp = mpd.commandExecutor.sendCommand("list albumartist file", song.file)
        return stripAlbumArtistTag(resp.firstOrNull().toString())
    }
}

enum class FileStatus {
    OK,
    MISSING,
    TAGS
}
