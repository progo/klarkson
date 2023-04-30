package utils.album
import klarksonmainframe.Persist
import klarksonmainframe.produceAlbums
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import utils.fixtures.TestTracks

class AlbumProductionTest {
    @Test
    fun `search all albums`() = runTest {
        val allAlbums =
            produceAlbums(TestTracks, query = "", limit = 100).toList()
        assertEquals(5, allAlbums.count())

        val songCount = allAlbums
            .map { alb -> alb.songs.count() }
            .sum()

        assertEquals(59, songCount) {
            "All songs in the database should be accounted for."
        }
    }

    @Test
    fun `search for Hipster Nonsense`() = runTest {
        val hipnos = produceAlbums(TestTracks, query = "Hipster nonsense").toList()

        assertEquals(2, hipnos.count()) {
            "Should have two albums of the name Hipster Nonsense"
        }

        // First album by Heisenberg
        val heisenberg = hipnos.first { a -> a.artist == "Heisenberg" }
        assertEquals("Hipster Nonsense", heisenberg.album)

        // Second is a VA, album artist Dunno
        val hipno = hipnos.first { a -> a.artist == "Dunno" }
        assertEquals(30, hipno.songs.count())
    }

    @Test
    fun `search albums that feature Pinku`() = runTest {
        val pinkuAlbums =
            produceAlbums(TestTracks, query = "Pinku").toList()

        assertEquals(3, pinkuAlbums.count()) {
            "There should be 3 albums that feature Pinku's music."
        }

        // Let's focus on the VA album, Hipster Nonsense
        val hipno = pinkuAlbums.first { alb -> alb.album == "Hipster Nonsense" }

        // this album should be constructed in its entirety
        assertEquals(30, hipno.songs.count()) {
            "The VA album Hipster Nonsense should always have 30 tracks."
        }
    }

    @Test
    fun `dont produce incomplete albums`() = runTest {
        val heisenAlbums =
            produceAlbums(TestTracks, query = "Heisenberg", limit=2).toList()
        assertEquals(1, heisenAlbums.count())

        val hipno = heisenAlbums.first()

        // this album should be constructed in its entirety
        assertEquals(8, hipno.songs.count()) {
            "Heisenberg's Hipster Nonsense should have 8 tracks."
        }
    }
}