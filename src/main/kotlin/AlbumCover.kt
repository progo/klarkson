package klarksonmainframe

import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

data class AlbumCover(val album: Album, var x: Int, var y: Int, val cover: BufferedImage?, val color: Color) : Comparable<AlbumCover> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as AlbumCover
        return (album === other.album)
    }

    override fun hashCode(): Int {
        return album.hashCode()
    }

    override operator fun compareTo(other: AlbumCover) : Int = compareXY(x, y, other.x, other.y)
}


object AlbumCoverImageService {
    private val iconCache = HashMap<Pair<AlbumCover, Int>, Image>()

    fun get(ac: AlbumCover, sz : Int) : ImageIcon? {
        if (ac.cover == null) {
            return null
        }

        val key = Pair(ac, sz)
        if (key !in iconCache) {
            iconCache[key] = ac.cover.getScaledInstance(sz, sz, Image.SCALE_SMOOTH)
        }

        return ImageIcon(iconCache[key])
    }
}