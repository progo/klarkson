package utils.fixtures

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import klarksonmainframe.MemoryTrackSource
import org.bff.javampd.song.MPDSong

// Prepare fixtures from external csv data.
private val songs = csvReader {
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
                .albumArtist(s["AlbumArtist"])
                .length(Integer.parseInt(s["Runtime"]))
                .file(s["File"] + madeupFilename)
                .build()
        }
}

val TestTracks = MemoryTrackSource(songs)
