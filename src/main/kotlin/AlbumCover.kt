package klarksonmainframe

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Image
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import java.io.File
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.swing.ImageIcon


data class AlbumCover(val album: Album) : Comparable<AlbumCover> {
    var x: Int = 0
    var y: Int = 0
    private val color: Color = Color(album.hashCode())
    var cover: BufferedImage = AlbumCoverImageService.coverLoadingImage as BufferedImage
    var loadingState: AlbumCoverLoadingStatus = AlbumCoverLoadingStatus.LOADING

    companion object {
        private val covergettingThreadPool : ExecutorService = Executors.newFixedThreadPool(2)
    }

    init {
        covergettingThreadPool.execute {
            initCover()
            AlbumCoverChangeNotificator.notifyListeners(listOf(this@AlbumCover))
        }
    }

    // TODO: this here plus the query stuff we have to refactor so that down
    // the pipeline we get information about state and perhaps other things.
    private fun initCover() {
        assert(loadingState == AlbumCoverLoadingStatus.LOADING)
        val path = getOrDownloadCover(album) ?: return
        val file = File(path)

        try {
            cover = ImageIO.read(file) as BufferedImage
            loadingState = AlbumCoverLoadingStatus.LOADED
        } catch (e : NullPointerException) {
            cover = AlbumCoverImageService.makePlaceholder(color) as BufferedImage
            loadingState = AlbumCoverLoadingStatus.MISSING
        }
    }

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


/**
 * Album has a cover image. It can be of three states:
 *
 * - Cover is loading (we show a placeholder)
 * - Cover is missing (we show a placeholder)
 * - Cover is loaded (we show it)
 */

enum class AlbumCoverLoadingStatus { LOADING, MISSING, LOADED }


object AlbumCoverImageService {
    // private val iconCache = HashMap<Triple<AlbumCover, Int, Boolean>, Image>()
    // nested 2-depth
    private val iconCache = HashMap<AlbumCover, HashMap<Pair<Int, Boolean>, Image>>()
    private val recordTemplateImage : BufferedImage = ImageIO.read(this.javaClass.classLoader.getResource("img/Record_Orange_400px.png")) as BufferedImage
    val coverLoadingImage: Image = recordTemplateImage

    private var cacheMisses = 0
    private var cacheAll = 0

    init {
        AlbumCoverChangeNotificator.registerListener { covers ->
            for (ac in covers) {
                iconCache.remove(ac)
            }
        }
    }

    fun get(ac: AlbumCover, sz: Int = 0, highlight: Boolean = false): Image {
        /*
        Cache scheme:
            level 1: key is [ac: AlbumCover], maps to a level 2 map
            level 2: key is Pair<Int, Boolean>, maps to an Image
        Or in pseudo:

        iconCache :: { AlbumCover: { (Int, Boolean): Image } }
        */

        cacheAll++
        val coverimg = ac.cover as Image
        val level2key = Pair(sz, highlight)

        // First we dig through the level 1
        if (ac !in iconCache) {
            iconCache[ac] = HashMap()
        }

        // Now we're at level 2
        val level2map = iconCache[ac] as HashMap

        if (level2key !in level2map) {
            var value: Image = coverimg

            // Resize
            if (sz > 0) {
                value = value.getScaledInstance(sz, sz, Image.SCALE_SMOOTH)
            }

            if (highlight) {
                value = RescaleOp(1.4f, 0f, null).filter(value as BufferedImage, null)
            }

            level2map[level2key] = value
            cacheMisses++
        }

        return level2map[level2key] as Image
    }

    fun getAsIcon(ac: AlbumCover, sz : Int = 0, highlight: Boolean = false): ImageIcon {
        return ImageIcon(get(ac, sz, highlight))
    }

    fun makePlaceholder(color: Color) : Image {
        // println("I don't run very often, do I? $sz, $color")
        val w = recordTemplateImage.width
        val h = recordTemplateImage.height
        val bi = BufferedImage(w, h, recordTemplateImage.type)

        val g : Graphics2D = bi.graphics.create() as Graphics2D
        g.drawImage(recordTemplateImage, 0, 0, null)

        // Draw a colored sleeve

        val r = w / 2.0
        val shape = Ellipse2D.Double(w - r/2, w / 2.0 - r/2, r, r)
        val shapecomp = Area(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))
        shapecomp.subtract(Area(shape))

        g.clip = shapecomp
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f)
        g.color = color
        g.fillRoundRect(0, 0, w, h, 30, 30)

        g.dispose()
        return bi
    }

    fun reportCacheStatistics() : String {
        val recursiveSize = iconCache.map { it.value.size }.sum()

//        val totalPixels = iconCache.map {
//            it.value.map {
//                val img = it.value as Image
//                img.height * img.width
//            }.sum()
//        }.sum()

        return buildString {
            append("Cache size = ${iconCache.size} covers and $recursiveSize cached bitmaps.\n")
            append("Cache hits = ${cacheAll - cacheMisses}, misses = $cacheMisses.\n")

            // very verbose...
            /*
            for (ac in iconCache.keys) {
                append("- ${ac.album.album}\n")
                val l2map = iconCache[ac] ?: continue
                for (l2 in l2map.keys) {
                    append("  - $l2 -> Image\n")
                }
            }
             */
        }
    }
}

object AlbumCoverChangeNotificator {
    private val listeners : MutableList<(List<AlbumCover>) -> Unit> = ArrayList()
    fun registerListener(block: (List<AlbumCover>) -> Unit) { listeners.add(block) }
    fun notifyListeners(covers : List<AlbumCover>) {
        // println("${covers.size} covers changed! Notifying ${listeners.size} listeners...")
        for (listener in listeners) {
            listener(covers)
        }
    }
}