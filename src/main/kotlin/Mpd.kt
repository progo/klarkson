package klarksonmainframe

import org.bff.javampd.server.Mpd
import org.bff.javampd.song.MpdSong
import org.bff.javampd.song.SongSearcher

object MpdServer {
    private val mpd = Mpd.Builder().build()

    fun getAlbums(count: Int = 100): List<Album> {
        val testSearch = "Mike Oldfield"
        val songs = mpd.songSearcher.search(SongSearcher.ScopeType.ARTIST, testSearch)
        return collectIntoAlbums(mpd, songs)
    }

    /**
     * Add given Songs to MPD playlist, and start playing them, if [play].
     */
    fun addTracks(tracks : Iterable<Song>, play : Boolean = false, quiet : Boolean = false) {
        val playlist = mpd.playlist
        val position = playlist.songList.size
        tracks.forEach { mpd.playlist.addSong(it.file) }

        // MpdSong.position is a zero-based index so if we want to play the
        // just-added content we find the MpdSong that has the [position].

        if (play) {
            val s : MpdSong = playlist.songList.first { it.position == position }
            mpd.player.playSong(s)
        }

        if (!quiet) {
            // NB [tracks] being iterable, count() may very well return 0 after consumption
            val msg = "${tracks.count()} tracks added!"
            showMessage(msg, timeMillis = 2000)
        }
    }

    fun addAlbums(albums: Iterable<AlbumCover>, play : Boolean = false) {
        val traxx = albums.flatMap { ac -> ac.album.songs }
        addTracks(traxx, play=play)
    }
}