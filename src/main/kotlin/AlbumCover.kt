package klarksonmainframe

import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
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
    private val iconCache = HashMap<Triple<AlbumCover, Int, Boolean>, Image>()

    fun get(ac: AlbumCover, sz : Int = 0, highlight : Boolean = false) : Image? {
        if (ac.cover == null) {
            return null
        }

        val key = Triple(ac, sz, highlight)

        if (key !in iconCache) {
            var value : Image = ac.cover

            // Resize
            if (sz > 0) {
                value = value.getScaledInstance(sz, sz, Image.SCALE_SMOOTH)
            }

            if (highlight) {
                value = RescaleOp(1.4f, 0f, null).filter(value as BufferedImage, null)
            }

            iconCache[key] = value
        }

        return iconCache[key]
    }

    fun getAsIcon(ac: AlbumCover, sz : Int = 0, highlight: Boolean = false): ImageIcon? {
        val img = get(ac, sz, highlight)
        return if (img == null) null else ImageIcon(img)
    }
}