package utils.mpd
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
}