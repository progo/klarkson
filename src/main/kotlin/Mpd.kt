package klarksonmainframe

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.ReceiveChannel
import org.bff.javampd.server.Mpd
import org.bff.javampd.song.MpdSong
import org.bff.javampd.song.SongSearcher
import kotlinx.coroutines.channels.produce

object MpdServer {
    private val mpd = Mpd.Builder().build()

    /**
    Make a producer that streams Album objects as they form from the search.
     */
    fun produceAlbums() : ReceiveChannel<Album> = MainScope().produce {
        val testSearch = "Pink Floyd"
        val mpdsongs = mpd.songSearcher.search(SongSearcher.ScopeType.ARTIST, testSearch)

        mpdsongs
            .asSequence()
            .map { song -> Song.read(song) }
            .groupBy { s -> Pair(s.albumArtist ?: s.artist, s.album) }
            .forEach {  (artistalbum, songs)  ->
                val (artist, album) = artistalbum
                send(Album.make(artist, album, songs))
            }
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
            val s : MpdSong = playlist.songList.first { it.position == position }
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
    fun getAlbumArtist(song: MpdSong) : String {
        val resp = mpd.commandExecutor.sendCommand("list albumartist file", song.file)
        return stripAlbumArtistTag(resp.firstOrNull().toString())
    }
}