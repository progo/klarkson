package klarksonmainframe

import java.awt.Color
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.*

class TrackList(lm : DefaultListModel<Song>) : JList<Song>(lm) {
    init {
        cellRenderer = makeCellRenderer()
        background = Color.DARK_GRAY
        foreground = Color.ORANGE
        selectionBackground = Color.BLACK
        selectionForeground = Color.ORANGE
        fixedCellHeight = 16
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION


        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(me : MouseEvent) {
                if (me.isPopupTrigger) {
                    object : JPopupMenu() {
                        init {
                            add(JMenuItem("Add").apply {
                                addActionListener { addSelectedTracks() }
                            })
                            add(JMenuItem("Play").apply {
                                addActionListener { addSelectedTracks(play=true) }
                            })
                        }
                    } .show(this@TrackList, me.x, me.y)
                }
            }
        })
    }

    private fun addSelectedTracks(play : Boolean = false) {
        val tracks = selectedValuesList.filter { it != SongSeparator }
        MpdServer.addTracks(tracks, play=play)
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
                val isSeparator = (song == SongSeparator)

                if (isSeparator) {
                    text = ""
                    icon = makeLineIcon(list.width, list.fixedCellHeight, list.foreground.darker())
                }
                else {
                    text = "[${song.runtime.toHuman()}] ${song.artist} - ${song.title}"
                    icon = null
                }

                if (isSelected && !isSeparator) {
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



fun makeLineIcon(width: Int, height: Int, color: Color) : ImageIcon {
    val b = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = b.graphics

    g.color = color
    g.drawLine(0, height / 2, width, height / 2)

    return ImageIcon(b)
}

