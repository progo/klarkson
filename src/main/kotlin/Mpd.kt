package klarksonmainframe

import org.bff.javampd.server.Mpd
import org.bff.javampd.song.SongSearcher

object MpdServer {
    private val mpd = Mpd.Builder().build()

    fun getAlbums(count: Int = 100): List<Album> {
        val testSearch = "Blade Runner"
        val songs = mpd.songSearcher.search(SongSearcher.ScopeType.ALBUM, testSearch)
        return collectIntoAlbums(mpd, songs)
    }

    fun addTracks(ts : Iterable<Song>, play : Boolean = false) {
        val playlist = mpd.playlist
        val tracks = playlist.songList.size
        ts.forEach { mpd.playlist.addSong(it.file) }

//        if (play) {
//            val f = ts.firstOrNull() ?: return
//            mpd.player.playSong()
//        }
    }
}