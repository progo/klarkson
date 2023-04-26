package klarksonmainframe.mpd

fun stripAlbumArtistTag(s: String) : String {
    val prefix = "AlbumArtist: "
    return if (s.startsWith(prefix)) s.substring(prefix.length) else s
}
