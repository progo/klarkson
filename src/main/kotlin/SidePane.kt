package klarksonmainframe

import java.awt.*
import javax.swing.*

class SidePane(
    albumSelection: AlbumSelection,
    menubar: JMenuBar,
    toolbar: JToolBar
) : JPanel() {
    private val txtArtist : JLabel
    private val txtAlbum : JLabel
    private val txtCoverImage : JLabel
    private var shownCover : AlbumCover? = null

    /*
    The layout is currently two nested BorderLayouts that ensure that borders are well populated.
     */

    init {
        layout = BorderLayout()
        background = Color.BLACK

        add(menubar, BorderLayout.NORTH)

        val albumshow = JPanel().apply {
            background = Color.BLACK
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

            txtArtist = JLabel("")
            txtAlbum = JLabel("")

            txtCoverImage = object : JLabel("") {
                override fun getMinimumSize(): Dimension = Dimension(this@SidePane.width, this@SidePane.width)
                override fun getMaximumSize(): Dimension = Dimension(this@SidePane.width, this@SidePane.width)
                override fun getPreferredSize(): Dimension = Dimension(this@SidePane.width, this@SidePane.width)
            }

            txtArtist.font = txtArtist.font.deriveFont(20F)
            txtArtist.isOpaque = true
            txtArtist.background = Color.ORANGE

            txtAlbum.foreground = Color.YELLOW
            txtAlbum.background = Color.BLACK
            txtAlbum.isOpaque = false

            add(txtCoverImage)
            add(txtArtist)
            add(txtAlbum)
        }

        val albumInboxList = DefaultListModel<AlbumCover>()
        val albumInboxScrolled = JScrollPane(AlbumInbox(albumInboxList))
        for (a in createAlbumCovers(500)) {
            albumInboxList.addElement(a)
        }

        val inner = JPanel().apply {
            layout = BorderLayout()

            add(albumshow, BorderLayout.NORTH)
            add(albumInboxScrolled, BorderLayout.CENTER)
            add(toolbar, BorderLayout.SOUTH)
        }

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
        txtAlbum.text = ""
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
            txtAlbum.text = ""
            txtArtist.text = ""
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