package klarksonmainframe

import java.awt.Color
import java.awt.Component
import javax.swing.*

class AlbumCoverList(listModel : DefaultListModel<AlbumCover>) : JList<AlbumCover>(listModel) {
    private val rowHeight : Int = 48

    init {
        cellRenderer = makeCellRenderer()
        fixedCellHeight = rowHeight

        background = Color.DARK_GRAY
        foreground = Color.ORANGE.darker()
        selectionBackground = Color.BLACK
        selectionForeground = Color.ORANGE
        componentPopupMenu = AlbumCoverListContextMenu(this)

        AlbumCoverChangeNotificator.registerListener { repaint() }
    }

    private fun makeCellRenderer() : ListCellRenderer<AlbumCover> {
        return object : JLabel(), ListCellRenderer<AlbumCover> {
            override fun getListCellRendererComponent(
                list: JList<out AlbumCover>,
                albumcover: AlbumCover,
                index: Int,
                isSelected: Boolean,
                hasFocus: Boolean
            ): Component {
                val album = albumcover.album
                text = "${album.artist} - ${album.album}"
                icon = AlbumCoverImageService.getAsIcon(albumcover, sz = rowHeight)

                if (isSelected) {
                    foreground = list.selectionForeground
                    background = list.selectionBackground
                } else {
                    foreground = list.foreground
                    background = list.background
                }

                isOpaque = true
                return this
            }
        }
    }
}


class AlbumCoverListContextMenu(private val listCmp : AlbumCoverList) : JPopupMenu() {
    init {
        add(JMenuItem("Play").apply {
            icon = ImageIcon(Resource.get("gf24/playback_play.png"))
            addActionListener {
                MpdServer.addAlbums(listCmp.selectedValuesList, play=true)
            }
        })
        add(JMenuItem("Append").apply {
            icon = ImageIcon(Resource.get("gf24/playback_play_plus.png"))
            addActionListener {
                MpdServer.addAlbums(listCmp.selectedValuesList, play=false)
            }
        })
    }

    /**
     * Show the context menu but also select whatever element might be
     * under the pointer.
     */
    override fun show(cmp: Component, x: Int, y: Int) {
        cmp as AlbumCoverList
        val row = cmp.realIndexUnderPoint(java.awt.Point(x, y))
        if (row >= 0) {
            cmp.selectedIndices += listOf(row)
        }
        super.show(cmp, x, y)
    }
}