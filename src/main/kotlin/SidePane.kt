package klarksonmainframe

import java.awt.*
import java.awt.image.BufferedImage
import java.lang.Math.abs
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import kotlin.random.Random

class SidePane(
    menubar: JMenuBar,
    toolbar: JToolBar
) : JPanel() {
    private val txtArtist = JLabel(" ")
    private val txtAlbum = JLabel(" ")
    private val txtCoverImage = object : JLabel("") {
        override fun getMinimumSize(): Dimension = Dimension(this@SidePane.width, this@SidePane.width)
        override fun getMaximumSize(): Dimension = Dimension(this@SidePane.width, this@SidePane.width)
        override fun getPreferredSize(): Dimension = Dimension(this@SidePane.width, this@SidePane.width)
    }

    private val tracksLM = DefaultListModel<Song> ()

    init {
        txtAlbum.font = txtArtist.font.deriveFont(20F)
        txtAlbum.foreground = Color.ORANGE

        txtArtist.foreground = Color.YELLOW
        txtArtist.background = Color.BLACK
    }

    private var shownCover : AlbumCover? = null

    // We use BorderLayouts here and for clarity all elements are added in at the end of init
    init {
        layout = BorderLayout()
        background = Color.BLACK

        val albumshow = JPanel().apply {
            background = Color.BLACK
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

            add(txtCoverImage)
            add(txtAlbum)
            add(txtArtist)
        }

        val albumshowAndPlayback = JPanel().apply {
            layout = BorderLayout()
            add(albumshow, BorderLayout.CENTER)
            add(toolbar, BorderLayout.SOUTH)
        }

        val tabbpane = JTabbedPane()
        val albumInboxList = DefaultListModel<AlbumCover>()
        val albumInboxScrolled = JScrollPane(AlbumInbox(albumInboxList))
        val trackst = JScrollPane(
            TrackList(tracksLM),
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        )

        fun updateAlbumInboxTitle() {
            val count = albumInboxList.size
            val msg = if (count == 0) "" else "($count)"
            tabbpane.setTitleAt(0, "Inbox $msg")
        }

        tabbpane.addTab("Inbox", albumInboxScrolled)
        tabbpane.addTab("Tracks", trackst)

        for (a in MpdServer.getAlbums()) {
            albumInboxList.addElement(a.createCover())
        }
        updateAlbumInboxTitle()

        val inner = JPanel().apply {
            layout = BorderLayout()

            add(albumshowAndPlayback, BorderLayout.NORTH)
            // add(albumInboxScrolled, BorderLayout.CENTER)
            add(tabbpane, BorderLayout.CENTER)
            // add(toolbar, BorderLayout.SOUTH)
        }

        add(menubar, BorderLayout.NORTH)
        add(inner, BorderLayout.CENTER)

        AlbumSelection.registerListener(::onAlbumSelection)
        AlbumCoverChangeNotificator.registerListener { showAlbum(shownCover) }
        albumInboxList.addListDataListener(object : ListDataListener {
            override fun intervalAdded(p0: ListDataEvent?) { updateAlbumInboxTitle() }
            override fun intervalRemoved(p0: ListDataEvent?) { updateAlbumInboxTitle() }
            override fun contentsChanged(p0: ListDataEvent?) { updateAlbumInboxTitle() }
        })
    }

    /**
     * This gets called when the user makes changes in selections
     */
    private fun onAlbumSelection(ass: AlbumSelection) {
        updateTracks(ass)
        when (ass.size()) {
            0 -> showAlbum(null)
            1 -> showAlbum(ass.first())
            else -> showAlbums(ass.toList())
        }
    }

    private fun updateTracks(albumcoverss: Iterable<AlbumCover>) {
        val tracks = albumcoverss.flatMap { ac -> ac.album.songs + listOf(SongSeparator) }
        tracksLM.clear()
        tracks.forEach { tracksLM.addElement(it) }
    }

    /**
     * Show several selected covers.
     */
    private fun showAlbums(covers: List<AlbumCover>) {
        shownCover = null
        txtArtist.text = " "
        txtAlbum.text = " "
        showCover(covers)
    }

    /**
     * Show a single album cover and details.
     */
    private fun showAlbum(c: AlbumCover?)  {
        shownCover = c

        if (c is AlbumCover) {
            val a = c.album
            txtArtist.text = a.artist
            txtAlbum.text = a.album
        }
        else {
            txtAlbum.text = " "
            txtArtist.text = " "
        }
        showCover(c)
    }

    /**
     * If [c] is null show a placeholder image, else the cover.
     */
    private fun showCover(c: AlbumCover?) {
        val image = c?.cover ?: AlbumCoverImageService.recordDecoration
        txtCoverImage.icon = ImageIcon(image.getScaledInstance(
            100.coerceAtLeast(txtCoverImage.width),
            100.coerceAtLeast(txtCoverImage.height),
            Image.SCALE_SMOOTH
        ))
    }

    /**
     * Collect and show at least some album covers [cs].
     */
    private fun showCover(cs: List<AlbumCover>) {
        val w = txtCoverImage.width.coerceAtLeast(100)
        txtCoverImage.icon = pileCovers(w, cs)
    }

    override fun paintComponent(g: Graphics) {
        showCover(shownCover)
        super.paintComponent(g)
    }
}


private fun pileCovers(size: Int, covers: List<AlbumCover>) : ImageIcon {
    val b = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = b.graphics as Graphics2D
    val sz = size / 2

    var i = 0
    println("-------------------------------------------------")

    // split the mini area into 4x4 grid
    for (cover in covers.take(12)) {
        if (i < 0) i = kotlin.math.abs(cover.hashCode() % 16)

        val row = (i / 4).toInt()
        val col = i % 4
        println ("$i -> ($col, $row)")

//        val randX = (cover.hashCode() % size).coerceIn(-sz/8, size - sz/2)
//        val randY = (cover.cover.hashCode() % size).coerceIn(-sz/8, size - sz/2)
        val x = col * size / 4
        val y = row * size / 4
        g.drawImage(cover.cover, x, y, sz, sz, null)
        i++
    }

    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
    g.color = Color.BLACK
    g.fillRect(0, 0, size, size)
    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
    g.font = Font("Monospace", Font.BOLD, 20)
    g.color = Color.ORANGE
    g.drawString("${covers.size} selected", 10, size/2)

    return ImageIcon(b)
}