package klarksonmainframe

import java.awt.Color
import java.awt.Component
import java.awt.Image
import javax.swing.*

/**
 * Implement a list of albums that are waiting to be added on the canvas.
 */
class AlbumInbox(lm : DefaultListModel<AlbumCover>) : JList<AlbumCover>(lm) {
    private val rowHeight : Int = 48

    init {
        cellRenderer = makeCellRenderer()
        fixedCellHeight = rowHeight

        background = Color.DARK_GRAY
        foreground = Color.ORANGE.darker()
        selectionBackground = Color.BLACK
        selectionForeground = Color.ORANGE
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

                if (albumcover.cover != null) {
                    icon = ImageIcon(albumcover.cover.getScaledInstance(rowHeight, rowHeight, Image.SCALE_SMOOTH))
                }

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