package klarksonmainframe

import org.bff.javampd.server.MPD
import org.bff.javampd.song.MPDSong
import org.bff.javampd.song.SongSearcher

interface TrackSource {
    // TODO. MPDSong is not an appropriate return here but we will do
    //  it when it becomes a burden.
    fun searchSongs(query: String) : Iterable<MPDSong>
}

/**
 * MPDTrackSource goes outside the process to reach MPD to find tracks.
 */
class MPDTrackSource (private val mpd: MPD) : TrackSource {
    override fun searchSongs(query: String): Iterable<MPDSong> {
        return mpd.songSearcher.search(
            SongSearcher.ScopeType.ANY,
            query)
    }
}


/**
 * MemoryTrackSource serves a prepopulated collection of songs, for testing
 * purposes.
 */
class MemoryTrackSource (private val traxx: Collection<MPDSong>) : TrackSource {
    private fun songAsString(song: MPDSong) = with(song) {
        "$artistName\t$name\t$title\t$albumArtist\t$albumName\t$file"
    }

    override fun searchSongs(query: String): Iterable<MPDSong> {
        return traxx.filter { song ->
            songAsString(song).contains(query, ignoreCase = true) }
    }
}