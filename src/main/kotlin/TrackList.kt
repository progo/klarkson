package klarksonmainframe

import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class TrackList(lm : DefaultListModel<Song>) : JList<Song>(lm) {
    init {
        cellRenderer = makeCellRenderer()
        background = Color.DARK_GRAY
        foreground = Color.ORANGE
        selectionBackground = Color.BLACK
        selectionForeground = Color.ORANGE
    }

    private fun makeCellRenderer() : ListCellRenderer<Song> {
        return object : JLabel(), ListCellRenderer<Song> {
            override fun getListCellRendererComponent(
                list: JList<out Song>,
                song: Song,
                index: Int,
                isSelected: Boolean,
                hasFocus: Boolean
            ): Component {
                text = "[${song.runtime.toHuman()}] ${song.artist} - ${song.title}"

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