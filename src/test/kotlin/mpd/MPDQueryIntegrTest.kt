package utils.mpd
import klarksonmainframe.MpdServer
import klarksonmainframe.Persist
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.test.runTest
import org.bff.javampd.server.MPD
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Query MPD to verify some behaviors.
 * Will require a running MPD with certain data.
 */
class MPDQueryIntegrTest {
    @Test
    fun `This JavaMPD library provides us with albumartist out of the box`() {
        val mpd = MPD.builder().build()
        val trax = mpd.songSearcher.searchAny("anjunabeats vol. 2")
        assertEquals(
            "Anjunabeats",
            trax.first().albumArtist
        )
    }

    @Test
    fun `Albumproducer will provide us with albumArtist data`() = runTest {
        Persist.initializeDatabase()
        val albums = MpdServer
            .produceAlbums("anjunabeats vol. 2")
            .toList()

        assertEquals(1, albums.count())

        val traxx = albums.first().songs
        assertEquals(
            "Anjunabeats",
            traxx.first().albumArtist
        )
    }
}