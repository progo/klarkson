package klarksonmainframe

import org.bff.javampd.server.Mpd
import org.bff.javampd.song.MpdSong

fun stripAlbumArtistTag(s: String) : String {
    val prefix = "AlbumArtist: "
    return if (s.startsWith(prefix)) s.substring(prefix.length) else s
}
