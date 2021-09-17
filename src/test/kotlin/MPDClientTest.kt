import org.bff.javampd.command.MpdCommand
import org.bff.javampd.server.Mpd
import org.bff.javampd.song.MpdSong
import org.bff.javampd.song.SongSearcher
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 *
 * Extension func
 */
val mpd = Mpd.Builder().build()

fun stripAlbumArtistTag(s: String) : String {
    val prefix = "AlbumArtist: "
    if (s.startsWith(prefix))
        return s.substring(prefix.length)
    else
        return s
}

fun MpdSong.getAlbumArtist() : String {
    val resp = mpd.commandExecutor.sendCommand("list albumartist file", this.toString())
    return stripAlbumArtistTag(resp.firstOrNull().toString())
}

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
        println("mpd connected = ${mpd.isConnected}")

        val status = mpd.player.status
        println("Player status = $status")

        val dpSongs = mpd.songSearcher.search(SongSearcher.ScopeType.ARTIST, "Daft Punk")

        // mpdsong :: MpdSong
        for (ds in dpSongs) {
            println("$ds\nArtist [${ds.artistName}], AlbumArtist [${ds.getAlbumArtist()}]")
        }


        mpd.close()
        println("----------------------------------")
    }

}