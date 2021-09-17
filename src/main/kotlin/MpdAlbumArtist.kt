package klarksonmainframe

import org.bff.javampd.server.Mpd
import org.bff.javampd.song.MpdSong

fun stripAlbumArtistTag(s: String) : String {
    val prefix = "AlbumArtist: "
    return if (s.startsWith(prefix)) s.substring(prefix.length) else s
}

/**
 * Extension functions can be used to organize this tool away
 */
fun Mpd.getAlbumArtist(song: MpdSong) : String {
    val resp = this.commandExecutor.sendCommand("list albumartist file", song.file)
    return stripAlbumArtistTag(resp.firstOrNull().toString())
}

