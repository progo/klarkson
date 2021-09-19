package klarksonmainframe

import java.awt.*
import javax.swing.*

class SidePane(
    albumSelection: AlbumSelection,
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

    init {
        txtArtist.font = txtArtist.font.deriveFont(20F)
        txtArtist.foreground = Color.ORANGE

        txtAlbum.foreground = Color.YELLOW
        txtAlbum.background = Color.BLACK
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
            add(txtArtist)
            add(txtAlbum)
        }

        val albumshowAndPlayback = JPanel().apply {
            layout = BorderLayout()
            add(albumshow, BorderLayout.CENTER)
            add(toolbar, BorderLayout.SOUTH)
        }

        val albumInboxList = DefaultListModel<AlbumCover>()
        val albumInboxScrolled = JScrollPane(AlbumInbox(albumInboxList))
        for (a in createAlbumCovers(100)) { albumInboxList.addElement(a) }

        val inner = JPanel().apply {
            layout = BorderLayout()

            add(albumshowAndPlayback, BorderLayout.NORTH)
            add(albumInboxScrolled, BorderLayout.CENTER)
            // add(toolbar, BorderLayout.SOUTH)
        }

        add(menubar, BorderLayout.NORTH)
        add(inner, BorderLayout.CENTER)

        albumSelection.registerListener(::onAlbumSelection)
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
    }

    private fun showAlbumCount(ass : AlbumSelection) {
        txtCoverImage.icon = null
        txtArtist.text = "${ass.size()} selected"
        txtAlbum.text = " "
    }

    private fun showAlbum(c: AlbumCover?)  {
        shownCover = c

        if (c is AlbumCover) {
            val a = c.album
            txtArtist.text = a.artist
            txtAlbum.text = a.album
            showCover(c)
        }
        else {
            txtAlbum.text = " "
            txtArtist.text = " "
            txtCoverImage.icon = null
        }
    }

    private fun showCover(c: AlbumCover?) {
        val coverImage = c?.cover
        if (coverImage != null) {
            txtCoverImage.icon = AlbumCoverImageService.get(c, sz = width)
        } else {
            txtCoverImage.icon = null
        }
    }

    override fun paintComponent(g: Graphics) {
        showCover(shownCover)
        super.paintComponent(g)
    }
}