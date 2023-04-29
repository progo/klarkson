package utils
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import klarksonmainframe.MemoryTrackSource
import org.bff.javampd.song.MPDSong
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// Prepare fixtures from external csv data.
val songs = csvReader {
    skipEmptyLine = true
}.let { rdr ->
    val dbfile = "fixtures/songdatabase.csv"
    rdr
        .readAllWithHeader(
            {}.javaClass.classLoader.getResource(dbfile)!!
                .readText())
        .map { s ->
            val cleantit = s["Title"]!!.replace(Regex("[^a-zA-Z0-9]"), "")
            val madeupFilename = "${s["Track"] ?: ""}_$cleantit.wav"
            MPDSong.builder()
                .artistName(s["Artist"])
                .albumName(s["Album"])
                .track(s["Track"])
                .discNumber(s["Disc"])
                .title(s["Title"])
                .length(Integer.parseInt(s["Runtime"]))
                .file(s["File"] + madeupFilename)
                .build()
    }
}


private val ts = MemoryTrackSource(songs)

class MemoryTrackSourceTest {
    @Test
    fun `see if we have built everything in memory`() {
        assertEquals(
            songs.count(),
            ts.searchSongs("").count()
        )
    }

    @Test
    fun `Test source should #searchSongs properly enough`() {
        val heisenbergs = ts.searchSongs("Heisenberg")
        assertEquals( 8, heisenbergs.count() ) {
            "should have 8 tracks of Heisenberg"
        }

        val mesopotaniaTracks = ts.searchSongs("Mesopotania")
        assertEquals(4, mesopotaniaTracks.count()) {
            "should have 4 tracks of Pinku album Mesopotania"
        }

        val pinkus = ts.searchSongs("pinku")
        assertEquals(16, pinkus.count()) {
            "should have 16 tracks of pinku (case insensitivity should be on)"
        }

        val hipster = ts.searchSongs("hipster nonsense")
        assertEquals(38, hipster.count()) {
            "should have 38 tracks of Hipster nonsense"
        }

        val searchbydir = ts.searchSongs("Pinku/Panorama")
        assertEquals(11, searchbydir.count())

        val noresults = ts.searchSongs("aybabtu")
        assertEquals(0, noresults.count())
    }
}