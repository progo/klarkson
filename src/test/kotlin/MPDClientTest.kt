package klarksonmainframe

import org.bff.javampd.server.Mpd
import org.bff.javampd.song.SongSearcher
import org.junit.Test
import kotlin.test.assertEquals


/**
 * http://finnyb.github.io/javampd/6.1.0-SNAPSHOT/apidocs/index.html
 */

class MPDClientTest {

    @Test
    fun smokeTest() {
        assertEquals (2, 1+1)
    }


    @Test
    /**
     * Prodding mpd interface
     */
    fun mpd() {
        println("----------------------------------")
        val mpd = Mpd.Builder().build()
        println("mpd connected = ${mpd.isConnected}")

        val player = mpd.player
        println("Player status = ${player.status}")
        println("Player plays = ${player.currentSong.file}")

        val dpSongs = mpd.songSearcher.search(SongSearcher.ScopeType.ARTIST, "Daft Punk")
        var length = 0
        for (ds in dpSongs) {
            length += ds.length
            println("$ds\nArtist [${ds.artistName}], AlbumArtist [${mpd.getAlbumArtist(ds)}]")
        }
        println("${length/60.0} minutes in total")

        mpd.close()
        println("----------------------------------")
    }

}