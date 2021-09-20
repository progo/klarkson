package klarksonmainframe

import java.awt.Color
import java.awt.Component
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

        // DnD, a complex beast.
        dragEnabled = true
        // And a singleton hack to go with it
        AlbumInboxSelection.setListComp(this)
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

// dirty hack to pass global STATE
object AlbumInboxSelection {
    private var listComp : JList<AlbumCover>? = null

    fun setListComp(lc : JList<AlbumCover>) {
        listComp = lc
    }

    fun getSelection() : List<AlbumCover> {
        val lc = listComp ?: return emptyList()
        return lc.selectedValuesList.toList()
    }

    fun deleteSelected() {
        val lc = listComp ?: return
        val lm : DefaultListModel<AlbumCover> = lc.model as DefaultListModel<AlbumCover>
        for (index in lc.selectedIndices.reversed()) {
            lm.remove(index)
        }
    }

}