package klarksonmainframe

import java.awt.*
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
import javax.swing.JTextArea
import kotlin.random.Random


data class AlbumCover(val album: Album) : Comparable<AlbumCover> {
    var x: Int = 0
    var y: Int = 0
    val color: Color = Color(album.hashCode())
    var cover: BufferedImage = AlbumCoverImageService.coverLoadingImage
    private var loadingState: AlbumCoverLoadingStatus = AlbumCoverLoadingStatus.LOADING

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
            cover = AlbumCoverImageService.makePlaceholder(this) as BufferedImage
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
    // nested 2-depth
    private val iconCache = HashMap<AlbumCover, HashMap<Pair<Int, Boolean>, BufferedImage>>()
    private val recordTemplateImage : BufferedImage = ImageIO.read(this.javaClass.classLoader.getResource("img/Record_Orange_400px.png"))
    val coverLoadingImage: BufferedImage = makeLoadingImage()

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
        val coverimg = ac.cover
        val level2key = Pair(sz, highlight)

        // First we dig through the level 1
        if (ac !in iconCache) {
            iconCache[ac] = HashMap()
        }

        // Now we're at level 2
        val level2map = iconCache[ac] as HashMap

        if (level2key !in level2map) {
            var value: BufferedImage = coverimg

            // Resize
            if (sz > 0) {
                val scaled = value.getScaledInstance(sz, sz, Image.SCALE_SMOOTH)
                value = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB).apply {
                    val g = graphics
                    g.drawImage(scaled, 0, 0, null)
                    g.dispose()
                }
            }

            if (highlight) {
                value = RescaleOp(1.4f, 0f, null).filter(value, null)
            }

            level2map[level2key] = value
            cacheMisses++
        }

        return level2map[level2key] as Image
    }

    fun getAsIcon(ac: AlbumCover, sz : Int = 0, highlight: Boolean = false): ImageIcon {
        return ImageIcon(get(ac, sz, highlight))
    }

    fun makePlaceholder(ac: AlbumCover) : Image {
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
        g.color = ac.color
        g.fillRoundRect(0, 0, w, h, 30, 30)

        // Little white label
        val labelX = 28 + Random.nextInt(0, 60)
        val labelY = 28 + Random.nextInt(0, 120)
        val labelW = 220
        val labelH = 120
        val labelClip = Rectangle2D.Double(labelX.toDouble(), labelY.toDouble(), labelW.toDouble(), labelH.toDouble())
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
        g.clip = null
        g.color = Color(220, 220, 220)
        g.fillRoundRect(labelX, labelY, labelW, labelH, 14, 14)
        g.color = Color(160, 160, 160)
        val lineheight = 13
        for (i in 1..8) {
            g.drawLine(labelX, labelY + 2 + i * lineheight, labelX + labelW - 1, labelY + 2 + i * lineheight)
        }
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)
        g.font = Font("Courier", Font.BOLD, 11)
        g.color = Color.BLACK
        g.clip = labelClip
        g.drawString(ac.album.artist, labelX + 2, labelY + 12)

        g.font = Font("Courier", Font.PLAIN, 11)
        JTextArea(ac.album.album).apply {
            val topmargin = 14
            lineWrap = true
            wrapStyleWord = true
            setBounds(labelX + 2, labelY + topmargin, labelW, labelH - topmargin)
            foreground = g.color
            background = Color(0,0,0,0)
            font = g.font
            val g2 = g.create(labelX + 2, labelY + topmargin, labelW, labelH - topmargin)
            // g2.clip = labelClip
            paint(g2)
            g2.dispose()
        }

        g.dispose()
        return bi
    }

    private fun makeLoadingImage() : BufferedImage {
        return BufferedImage(
            recordTemplateImage.width,
            recordTemplateImage.height,
            BufferedImage.TYPE_INT_ARGB
        ).apply {
            val g = graphics
            g.drawImage(recordTemplateImage, 0, 0, null)

            g.color = Color.BLACK
            g.fillRoundRect(20, 20, 75, 15, 5, 5)
            g.color = Color.WHITE
            g.drawString("Loading...", 28, 32)
            g.dispose()
        }
    }

    fun reportCacheStatistics() : String {
        val recursiveSize = iconCache.map { it.value.size }.sum()

        val totalBytes = iconCache.map { lev1entry ->
            lev1entry.value.map { lev2entry ->
                val img = lev2entry.value
                img.colorModel.pixelSize / 8.0 * img.height * img.width
            }.sum()
        }.sum()

        return buildString {
            append("Cache size = ${iconCache.size} covers " +
                    "and $recursiveSize cached bitmaps, " +
                    "totalling ${(totalBytes/1024/1024).format(2)} MiB.\n")
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