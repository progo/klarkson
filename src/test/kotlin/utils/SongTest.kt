package utils

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import klarksonmainframe.MpdServer
import klarksonmainframe.Song
import org.bff.javampd.song.MPDSong
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SongTest {
    @Test
    fun `#Song() should be able to create with null discNumber`() {
        val s = Song(
            artist = "artist",
            album = "album",
            title = "title",
            file = "/foobar/",
            albumArtist = null,
            runtime = 42,
            discNumber = null,
            comment = null,
            genre = null,
            trackNumber = 0,
            year = 2000
        )
    }

    @Test
    fun `#Song#make() should be able to create a Song with null discNumber`() {
        val mpdsong = MPDSong
            .builder()
            .artistName("Artist")
            .albumName("Album")
            .title("Title")
            .date(null)
            .discNumber(null)
            .track("4")
            .file("/foo/bar.flac")
            .build()

        Song.make(mpdsong)
    }
}