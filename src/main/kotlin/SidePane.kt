package klarksonmainframe

import java.awt.*
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

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
        when (ass.size()) {
            0 -> showAlbum(null)
            1 -> showAlbum(ass.first())
            else -> showAlbumCount(ass)
        }

        updateTracks(ass)
    }

    private fun updateTracks(albumcoverss: Iterable<AlbumCover>) {
        val tracks = albumcoverss.flatMap { ac -> ac.album.songs + listOf(SongSeparator) }
        tracksLM.clear()
        tracks.forEach { tracksLM.addElement(it) }
    }

    private fun showAlbumCount(ass : AlbumSelection) {
        showCover(null)
        txtArtist.text = "${ass.size()} selected"
        txtAlbum.text = " "
    }

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

    override fun paintComponent(g: Graphics) {
        showCover(shownCover)
        super.paintComponent(g)
    }
}