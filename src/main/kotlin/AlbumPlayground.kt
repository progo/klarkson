package klarksonmainframe

import klarksonmainframe.ui.SearchBoxActivator
import klarksonmainframe.utils.median
import java.awt.*
import java.awt.dnd.*
import java.awt.event.*
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.math.*
import kotlin.random.Random
import kotlin.system.measureNanoTime

const val ALBUM_COVER_SIZE = 200
private val ZOOM_SCALE_FACTORS = doubleArrayOf(0.05, 0.10, 0.20, 0.50, 1.00, 1.50, 2.00)
private enum class ZoomDirection (val value: Int) {
    IN (1),
    OUT (-1)
}

enum class Direction {
    LEFT, RIGHT, UP, DOWN
}

/**
 * Track selected albums and offers notifications to listeners.
 */
object AlbumSelection : Iterable<AlbumCover> {
    private val selection : MutableSet<AlbumCover> = HashSet()
    private val listeners : MutableList<(AlbumSelection) -> Unit> = ArrayList()
    private val listeners1time : MutableList<(AlbumSelection) -> Unit> = ArrayList()

    /**
     * Interface things
     */

    fun size() = selection.size
    fun selectedAny() = selection.size > 0
    operator fun contains(c: AlbumCover) = c in selection
    override operator fun iterator() : Iterator<AlbumCover> = selection.iterator()

    /**
     * These operations trigger listener events
     */
    fun replace(albs: Collection<AlbumCover>) {
        selection.clear()
        selection.addAll(albs)
        listenerCallback()
    }

    fun clear() {
        selection.clear()
        listenerCallback()
    }

    fun add(c: AlbumCover) {
        selection.add(c)
        listenerCallback()
    }

    fun toggle(c: AlbumCover) {
        if (c in selection) {
            selection.remove(c)
        } else {
            selection.add(c)
        }
        listenerCallback()
    }

    /**
     * Poor listener mechanism. Swing and Java is full of examples of simple mechanims.
     */
    fun registerListener(func : (AlbumSelection) -> Unit) {
        listeners.add(func)
    }
    fun registerOnetimeListener(func : (AlbumSelection) -> Unit) {
        listeners1time.add(func)
    }
    private fun listenerCallback() {
        val onetimelisteners = listeners1time.toList()
        listeners1time.clear()
        onetimelisteners.forEach { it(this) }
        listeners.forEach { it(this) }
    }
}


class AlbumPlayground(private val albums : AlbumOrganizer): JPanel(), KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, DropTargetListener {
    init {
        background = Color(225, 205, 40)
        DropTarget(this, this)

        AlbumCoverChangeNotificator.registerListener { repaint() }
        AlbumSelection.registerListener { repaint() }

        albums.registerSearchEventListener(object : SearchEventHandler {
            private var searchResults: SearchResults? = null
            override fun newSearch(results: SearchResults) {
                searchResults = results
                nextResult()
            }

            override fun nextResult() {
                val cover = searchResults?.next(cycle=true) ?: return
                AlbumSelection.replace(setOf(cover))
                centerAroundCover(cover)
            }
        })
    }

    private val coversOnTheMove: MutableSet<AlbumCover> = HashSet()
    private val coversOnTheDrag: MutableList<AlbumCover> = mutableListOf()
    private var coverDragPoint: Point? = null

    /**
     * Record an altered album cover here so that its coordinate changes can be saved in batch.
     */
    private val saveQueue: MutableSet<AlbumCover> = HashSet()
    private val saveTimer = Timer(5000) {
        albums.save()
        saveQueue.clear()
        repaint()
    }.apply {
        isRepeats = false
    }

    private fun saveCoverDelayed(ac: AlbumCover) { saveCoverDelayed(setOf(ac)) }
    private fun saveCoverDelayed(ac: Iterable<AlbumCover>) {
        saveQueue.addAll(ac)
        // Restart instead of start: start counting from zero
        saveTimer.restart()
    }


    private var viewportX = 0
    private var viewportY = 0
    private var lastMouseX = -1
    private var lastMouseY = -1
    private var viewportPan = false
    private var viewportScaleFactor = 1.00
    private var viewportScaleIndex : Int = ZOOM_SCALE_FACTORS.indexOfFirst { it == viewportScaleFactor }
    private var mouseMoveTimer : Timer = Timer(1000) { }

    private var selectionEndPoint : Point? = null
    private var selectionStartPoint : Point? = null

    private fun paintBackground(g: Graphics) {
        val w = width
        val h = height
        val spacing = 10
        var x = 0

//        val color = background.brighter()
//        val color2 = background.darker()
//
//        for (i in (1..10)) {
//            g.color = if (Random.nextBoolean()) { color } else { color2 }
//            g.fillOval(
//                Random.nextInt(0, width),
//                Random.nextInt(0, height),
//                Random.nextInt(50, 250),
//                Random.nextInt(50, 250)
//            )
//        }

//        g.color = Color.darkGray.brighter().brighter()
//        while (x < width * 2.5) {
//            g.drawLine(0, x, x, 0)
//            x += spacing
//        }
    }

    /**
     * Physical on-screen coordinates into virtual ones.
     */
    private fun physical2virtual(x: Int, y: Int): Point {
        val xv = viewportX - (x - width/2) / viewportScaleFactor
        val yv = viewportY - (y - height/2) / viewportScaleFactor
        return Point(xv, yv)
    }
    private fun physical2virtual(p: Point): Point = physical2virtual(p.x, p.y)
    private fun physical2virtual(p: java.awt.Point) = physical2virtual(p.x, p.y)

    /**
     * Virtual on-screen coordinates into physical ones.
     */
    private fun virtual2physical(x: Int, y: Int): Point {
        val xp = (viewportX - x) * viewportScaleFactor + (width / 2)
        val yp = (viewportY - y) * viewportScaleFactor + (height / 2)
        return Point(xp, yp)
    }
    private fun virtual2physical(p: Point): Point = virtual2physical(p.x, p.y)

    /**
     * See if virtual points [x], [y] are currently visible in the viewport.
     */
    private fun visibleInView(x: Int, y: Int) : Boolean {
        val (px, py) = virtual2physical(x, y)
        return (0 < px) and (px < width) and (0 < py) and (py < height)
    }

    private fun paintAlbums(g: Graphics2D, albums : Iterable<AlbumCover>, highlight : Boolean = false) {
        // looks good -- 5000 blank albums or 850 pictured albums poses no sweat

        val coverside = (viewportScaleFactor * ALBUM_COVER_SIZE).toInt()

        if (highlight) {
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
        }

        for (albumcover in albums) {
            val (xp, yp) = virtual2physical(albumcover.x, albumcover.y) - (coverside / 2)

            g.drawImage(albumcover.cover, xp, yp, coverside, coverside, null)

            if (albumcover in AlbumSelection) {
                g.color = Color.BLACK
                g.stroke = BasicStroke(3F)
                g.drawRect(xp, yp, coverside, coverside)
            }
        }

        // Restore
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
    }

    private fun lassoRectangle(a: Point, b: Point) : Pair<Point, Point> {
        val x1 = min(a.x, b.x)
        val y1 = min(a.y, b.y)
        val x2 = max(a.x, b.x)
        val y2 = max(a.y, b.y)
        return Pair(Point(x1, y1), Point(x2, y2))
    }

    private fun paintLasso(g: Graphics2D) {
        val (a, b) = lassoRectangle(
            selectionStartPoint ?: return,
            selectionEndPoint ?: return
        )
        val width = b.x - a.x
        val height = b.y - a.y

        g.color = Color.BLUE
        g.stroke = BasicStroke(2F)
        g.drawRoundRect(a.x, a.y, width, height, 10, 10)
    }

    private fun paintCrosshair(g: Graphics2D) {
        val cx_length = 10
        g.color = Color.BLACK
        g.stroke = BasicStroke(1F)
        g.drawLine(width / 2, height / 2 - cx_length, width / 2, height / 2 + cx_length)
        g.drawLine(width / 2 - cx_length, height / 2, width / 2 + cx_length, height / 2)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D

        val paint_time_ms = 1e-6 * measureNanoTime {
            paintBackground(g2)
            paintAlbums(g2, albums)
            // paintCrosshair(g2)
            paintLasso(g2)
            paintAlbums(g2, coversOnTheDrag, highlight = true)
        }

        // Debug messages
        g2.color = Color.BLACK
        g2.fillRect(0, 0, width, 14)
        g2.color = Color.YELLOW
        g2.drawString( "Zoom level ${(viewportScaleFactor * 100).toInt()}%. " +
                "Viewport: ($viewportX, $viewportY). " +
                "${albums.size()} covers. " +
                "${saveQueue.size} unsaved. " +
                "Frame took ${"%.2f".format(paint_time_ms)}ms", 0, 10)
    }

    /**
     * If we have one album selected then we will allow this navigation
     */
    private fun findAdjacentAlbumCover(dir: Direction) {
        if (AlbumSelection.size() != 1)
            return

        val selected = AlbumSelection.first()

        val alb = albums.getAlbumInTheDirectionOf(Point(selected.x, selected.y), dir)
        if (alb != null) {
            AlbumSelection.replace(setOf(alb))

            val x = alb.x
            val y = alb.y
            if (!visibleInView(x, y))
                centerAroundPoint(x, y)
        }
    }

    override fun keyPressed(ke: KeyEvent) {
        val shift = if (ke.isShiftDown) { 200 } else { 100 }
        when (ke.keyCode) {
            KeyEvent.VK_R -> jumpToRandomSpot()
            KeyEvent.VK_HOME -> jumpToRandomSpot()
            KeyEvent.VK_LEFT -> findAdjacentAlbumCover(Direction.LEFT)
            KeyEvent.VK_RIGHT -> findAdjacentAlbumCover(Direction.RIGHT)
            KeyEvent.VK_UP -> findAdjacentAlbumCover(Direction.UP)
            KeyEvent.VK_DOWN -> findAdjacentAlbumCover(Direction.DOWN)
            KeyEvent.VK_ENTER -> centerAroundSelected()
            KeyEvent.VK_SPACE -> bringSelectedCoversTogether()

            KeyEvent.VK_DELETE -> removeSelectedCovers()
        }

        if (ke.keyCode == KeyEvent.VK_SLASH || (ke.isControlDown && ke.keyCode == KeyEvent.VK_F)) {
            SearchBoxActivator.focus()
        }

        repaint()
    }

    private fun removeSelectedCovers() {
        if (!AlbumSelection.selectedAny()) {
            return
        }

        albums.removeAlbums(AlbumSelection)
    }

    private fun zoomIn(x: Int, y: Int) {
        val (vx, vy) = physical2virtual(x, y)
        adjustViewportScale(ZoomDirection.IN)
        centerAroundPoint(vx, vy)
    }

    private fun zoomOut(x: Int, y: Int) {
        adjustViewportScale(ZoomDirection.OUT)
    }

    /** Adjust scaling and recenter around x, y.
     * Return true if scale was adjusted, false if nothing happened.  */
    private fun adjustViewportScale(direction: ZoomDirection) {
        // print("[$viewportScaleIndex] Zooming $direction, ")

        val newIndex = (viewportScaleIndex + direction.value).coerceIn(0, ZOOM_SCALE_FACTORS.size - 1)
        val newScale = ZOOM_SCALE_FACTORS[newIndex]
        // println("from $viewportScaleFactor to $newScale.")

        val dS = newScale - viewportScaleFactor
        if (abs(dS) == 0.0) {
            return
        }

        var count = 0
        val steps = 10

        Timer(10) { ae: ActionEvent ->
            viewportScaleFactor += dS / steps

            count += 1
            if (count >= steps) {
                viewportScaleFactor = newScale
                viewportScaleIndex = newIndex
                val t = ae.source as Timer
                t.stop()
            }

            repaint()
        } .apply {
            start()
        }
    }

    override fun keyReleased(ke: KeyEvent) { }
    override fun keyTyped(ke: KeyEvent) { }

    /*
     *  Three mouse move operations that mark the most significant operations in the GUI
     */
    override fun mousePressed(me: MouseEvent) {
        if (me.isShiftDown) {
            when (me.button) {
                MouseEvent.BUTTON1 -> beginLasso(me)
            }
        }
        else {
            when (me.button) {
                MouseEvent.BUTTON1 -> beginMoveCover(me)
                MouseEvent.BUTTON3 -> beginPan(me)
            }
        }
    }

    override fun mouseReleased(me: MouseEvent) {
        endPan(me)
        endMoveCover()
        endLasso()
    }

    override fun mouseDragged(me: MouseEvent) {
        duringPan(me)
        duringMoveCover(me)
        if (me.isShiftDown) {
            duringLasso(me)
        }
    }

    /**
     * Making selections : lasso
     */
    private fun beginLasso(me: MouseEvent) {
        selectionStartPoint = Point(me.x, me.y)
        selectionEndPoint = Point(me.x, me.y)
    }

    private fun duringLasso(me: MouseEvent) {
        selectionEndPoint = Point(me.x, me.y)

        val (a, b) = lassoRectangle(
            selectionStartPoint ?: return,
            selectionEndPoint ?: return
        )

        val albs = albums.allAlbumsWithinRegion(physical2virtual(a), physical2virtual(b))
        AlbumSelection.replace(albs)
    }

    private fun endLasso() {
        clearLasso()
    }

    private fun clearLasso() {
        selectionStartPoint = null
        selectionEndPoint = null
        repaint()
    }

    private fun clearSelection() {
        AlbumSelection.clear()
    }

    /**
     * When there are two or more albums selected, bring them together around
     * the center point. Don't worry about existing albums around the area: keep
     * all selected so that user takes it from there.
     */
    private fun bringSelectedCoversTogether() {
        if (AlbumSelection.size() < 2)  return

        val sz = AlbumSelection.size().toDouble()

        val rows : Int = sqrt(sz).roundToInt()
        val cols : Int = (sz / rows).roundToInt()

        val points = AlbumSelection.map { ac -> Point(ac.x, ac.y) }
        val xs = points.map { it.x }
        val ys = points.map { it.y }

        // the second term is some voodoo that tries to keep an offset from happening
        val centerX = median(xs) - (ALBUM_COVER_SIZE * cols/4.0).roundToInt()
        val centerY = median(ys) - (ALBUM_COVER_SIZE * rows/4.0).roundToInt()

        var row = 0
        var col = 0

        for (album in AlbumSelection) {
            album.x = centerX + row * ALBUM_COVER_SIZE + Random.nextInt(2, 10)
            album.y = centerY + col * ALBUM_COVER_SIZE + Random.nextInt(2, 10)

            col += 1
            if (col >= cols) {
                col = 0
                row += 1
            }
        }

        saveCoverDelayed(AlbumSelection)
        albums.reorganize()
        repaint()
    }

    /**
     * Moving of covers
     */
    private fun beginMoveCover(me: MouseEvent) {
        val (x, y) = physical2virtual(me.x, me.y)
        val alb = albums.getByLocation(x, y) ?: return;

        coversOnTheMove.clear()
        if (alb in AlbumSelection) {
            coversOnTheMove.addAll(AlbumSelection)
        } else {
            coversOnTheMove.add(alb)
        }

        lastMouseY = me.y
        lastMouseX = me.x
    }

    /**
     * Move ongoing
     */
    private fun duringMoveCover(me: MouseEvent) {
        val pdx = lastMouseX - me.x
        val pdy = lastMouseY - me.y

        for (cotm in coversOnTheMove) {
            val (x, y) = virtual2physical(cotm.x, cotm.y)
            var (xx, yy) = physical2virtual(x - pdx, y - pdy)
            val vdx = xx - cotm.x
            val vdy = yy - cotm.y

            // Manually correct rounding errors when zoomed in
            if (abs(pdx) > 0 && abs(vdx) == 0) {
                xx += sign(pdx.toDouble()).toInt()
            }
            if (abs(pdy) > 0 && abs(vdy) == 0) {
                yy += sign(pdy.toDouble()).toInt()
            }

            cotm.x = xx
            cotm.y = yy
        }

        lastMouseY = me.y
        lastMouseX = me.x
        albums.reorganize()
        repaint()
    }

    /**
     * Move is finished.
     */
    private fun endMoveCover() {
        saveCoverDelayed(coversOnTheMove)
        coversOnTheMove.clear()
        albums.reorganize()
        repaint()
    }

    /**
     * begin pan operation
     */
    private fun beginPan(me: MouseEvent) {
        viewportPan = true
        lastMouseY = me.y
        lastMouseX = me.x
    }

    /**
     * And here we end it.
     */
    private fun endPan(me: MouseEvent) {
        viewportPan = false
        requestFocusInWindow()
    }

    private fun duringPan(me: MouseEvent) {
        if (! viewportPan) {
            return
        }

        val scale = 2 / viewportScaleFactor

        val dx = lastMouseX - me.x
        val dy = lastMouseY - me.y

        viewportX += (dx * scale).toInt()
        viewportY += (dy * scale).toInt()

        lastMouseX = me.x
        lastMouseY = me.y

        repaint()
    }

    override fun mouseClicked(me: MouseEvent) {
        requestFocusInWindow()
        val (x, y) = physical2virtual(me.x, me.y)

        if (me.clickCount == 2) {
            centerAroundPoint(x, y)
            return
        }

        val alb = albums.getByLocation(x, y)
        if (alb == null) {
            clearSelection()
            return
        }
        if (me.isControlDown) {
            AlbumSelection.toggle(alb)
        }
        else {
            AlbumSelection.replace(setOf(alb))
        }
    }

    /**
     * Center the playground around this VIRTUAL point.
     */
    private fun centerAroundPoint(x: Int, y: Int, animate: Boolean = true) {
        if (! animate) {
            viewportX = x
            viewportY = y
            return
        }

        val dx = x - viewportX
        val dy = y - viewportY

        val animationDurationMs = 200
        val steps = 50
        var count = 0

        Timer(animationDurationMs / steps) { ae: ActionEvent ->
            viewportX += (dx / steps)
            viewportY += (dy / steps)

            count += 1
            if (count >= steps) {
                // Make sure we hit the exact target when we finish.
                viewportX = x
                viewportY = y
                val t = ae.source as Timer
                t.stop()
            }

            repaint()
        }.apply {
            start()
        }
    }

    private fun centerAroundCover(ac: AlbumCover?, animate: Boolean = true) {
        val cover = ac ?: return
        centerAroundPoint(ac.x, ac.y, animate)
    }

    private fun jumpToRandomSpot(select: Boolean = true) {
        val cover = albums.shuffled().firstOrNull() ?: return
        if (select)
            AlbumSelection.replace(listOf(cover))
        centerAroundCover(cover)
    }

    /**
     * Center around selected album
     */
    private fun centerAroundSelected() {
        if (AlbumSelection.size() != 1)
            return

        val alb = AlbumSelection.first()
        centerAroundPoint(alb.x, alb.y)
    }

    override fun mouseMoved(me: MouseEvent) {
        if (!mouseMoveTimer.isRunning) {
            val (x, y) = physical2virtual(me.x, me.y)
            mouseMoveTimer = Timer(25) {
                //println(Point(x, y).toString() + " -> " + lookup_cover_at_coords(x, y))
            }
            mouseMoveTimer.initialDelay = 25
            mouseMoveTimer.isRepeats = false
            mouseMoveTimer.restart()
        }
    }

    override fun mouseEntered(p0: MouseEvent?) { }
    override fun mouseExited(p0: MouseEvent?) { }
    override fun mouseWheelMoved(mwe: MouseWheelEvent) {
        requestFocusInWindow()
        if (mwe.wheelRotation < 0) {
            zoomIn(mwe.x, mwe.y)
        } else {
            zoomOut(mwe.x, mwe.y)
        }
    }

    /**
     * Accept drag-n-drops  from other components. We are focused on the JList
     * that houses AlbumCovers and our code assumes it blindly, at the risk of
     * numerous bugs.
     *
     * Swing and AWT have very cool DnD mechanisms in place, but they're very
     * complex and for this implementation we are well served with a dead
     * simple, if hacky, mechanism with little elegance.
     */
    override fun drop(dtde: DropTargetDropEvent) {
        val (vx, vy) = physical2virtual(dtde.location)

        val randomize = coversOnTheDrag.size > 1

        for (a in coversOnTheDrag) {
            a.x = vx + if (randomize) Random.nextInt(-15, 15) else 0
            a.y = vy + if (randomize) Random.nextInt(-15, 15) else 0
            albums.put(a)
        }

        albums.reorganize()
        finishDragNDrop()
        // repaint()

        AlbumInboxSelection.acceptDragNDrop()
        requestFocusInWindow()
    }

    private fun finishDragNDrop() {
        saveCoverDelayed(coversOnTheDrag)
        coversOnTheDrag.clear()
        coverDragPoint = null
        repaint()
    }

    override fun dragOver(dtde: DropTargetDragEvent) {
        coverDragPoint = Point(dtde.location)
        val (vx, vy) = physical2virtual(dtde.location)
        var shift = 0
        for (a in coversOnTheDrag) {
            a.x = vx + shift
            a.y = vy + shift
            shift += 8
        }
        repaint()
    }

    override fun dropActionChanged(p0: DropTargetDragEvent?) { }
    override fun dragEnter(dtde: DropTargetDragEvent) {
        coversOnTheDrag.addAll(AlbumInboxSelection.getSelection())
        coverDragPoint = Point(dtde.location)
    }

    override fun dragExit(p0: DropTargetEvent?) {
        finishDragNDrop()
    }
}