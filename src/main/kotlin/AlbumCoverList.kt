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