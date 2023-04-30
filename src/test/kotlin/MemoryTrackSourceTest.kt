package utils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import utils.fixtures.TestTracks


class MemoryTrackSourceTest {
    @Test
    fun `see if we have built everything in memory`() {
        assertEquals(
            59,
            TestTracks.searchSongs("").count()
        )
    }

    @Test
    fun `Test source should #searchSongs properly enough`() {
        val heisenbergs = TestTracks.searchSongs("Heisenberg")
        assertEquals( 8, heisenbergs.count() ) {
            "should have 8 tracks of Heisenberg"
        }

        val mesopotaniaTracks = TestTracks.searchSongs("Mesopotania")
        assertEquals(4, mesopotaniaTracks.count()) {
            "should have 4 tracks of Pinku album Mesopotania"
        }

        val pinkus = TestTracks.searchSongs("pinku")
        assertEquals(16, pinkus.count()) {
            "should have 16 tracks of pinku (case insensitivity should be on)"
        }

        val hipster = TestTracks.searchSongs("hipster nonsense")
        assertEquals(38, hipster.count()) {
            "should have 38 tracks of Hipster nonsense"
        }

        val searchbydir = TestTracks.searchSongs("Pinku/Panorama")
        assertEquals(11, searchbydir.count())

        val noresults = TestTracks.searchSongs("aybabtu")
        assertEquals(0, noresults.count())

        val hentai = TestTracks.searchSongs("sapporo hentai")
        assertEquals("Dunno", hentai.first().albumArtist)
    }
}