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
        println("MPD connected = ${mpd.isConnected}")

        val player = mpd.player
        println("Player status = ${player.status}")
        println("Player playing = `${player.currentSong.file}`")

        mpd.close()
        println("----------------------------------")
    }


    @Test
    fun collectSongs() {
        println("----------------------------------")
        val mpd = Mpd.Builder().build()

        val dpSongs = mpd.songSearcher.search(SongSearcher.ScopeType.ARTIST, "Daft Punk")
        val songs = dpSongs.map { song -> Song.read(mpd, song) }.toList()

        for (s in songs) {
            println("$s")
        }

        println("\n===\n")

        val albums = songs.groupBy { s -> Pair(s.albumArtist ?: s.artist, s.album) }

        for (a in albums) {
            println(a)
        }

        println("\n===\n")

        val alb2 = collectIntoAlbums(mpd, dpSongs)
        for (a in alb2) {
            println("${a.album} is ${a.runtime.toMinutes()} minutes, or [${a.runtime.toHuman()}].")
        }

        mpd.close()
        println("----------------------------------")
    }

    @Test
    fun testVAAlbums() {
        println("----------------------------------")
        val mpd = Mpd.Builder().build()

        val testSearch = "Blade Runner"

        val songs = mpd.songSearcher.search(SongSearcher.ScopeType.ALBUM, testSearch)
        val albums = collectIntoAlbums(mpd, songs)

        println("*** For search \"$testSearch\" we got ${albums.size} albums.")

        for (a in albums) {
            println("- ${a.album} by `${a.artist}' is ${a.runtime.toMinutes()} minutes, or [${a.runtime.toHuman()}] over ${a.songs.size} tracks.")
            for (s in a.songs) {
                println("  - ${s.trackNumber}/${s.discNumber ?: 1} [${s.artist}] - [${s.title}]")
            }
        }

        mpd.close()
        println("----------------------------------")
    }
}