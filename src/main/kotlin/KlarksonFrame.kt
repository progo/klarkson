package klarksonmainframe

import java.awt.Color
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI
import kotlin.system.exitProcess

data class Album(val artist: String, val album: String, val year: Int)

data class AlbumCover(val album: Album, var x: Int, var y: Int, val cover: BufferedImage?, val color: Color) : Comparable<AlbumCover> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as AlbumCover
        return (album === other.album)
    }

    override fun hashCode(): Int {
        return album.hashCode()
    }

    override operator fun compareTo(other: AlbumCover) : Int = compareXY(x, y, other.x, other.y)
}

class KlarksonFrame : JFrame() {
    private lateinit var splitpane : JSplitPane
    private val DEFAULT_SPLIT_LOCATION = 200

    init {
        createUI()
    }

    private fun createMenuBar(): JMenuBar {
        return JMenuBar().apply {
            background = Color.ORANGE
            foreground = Color.WHITE
            isOpaque = true

            val file = JMenu("Klarkson")
            file.mnemonic = KeyEvent.VK_K

            val eMenuItem = JMenuItem("Exit")
            eMenuItem.mnemonic = KeyEvent.VK_X
            eMenuItem.toolTipText = "Exit application"
            eMenuItem.addActionListener { exitProcess(0) }

            file.add(eMenuItem)
            add(file)
        }
    }

    private fun resetSplitPane() {
        splitpane.apply {
            dividerLocation = size.width - DEFAULT_SPLIT_LOCATION - 3
        }
    }

    private fun createAndAdjustSplitPane(left: JComponent, right: JComponent): JSplitPane {
        return JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right).apply {
            dividerSize = 3

            ui = object : BasicSplitPaneUI() {
                override fun createDefaultDivider(): BasicSplitPaneDivider {
                    return object : BasicSplitPaneDivider(this) {
                        override fun paint(g: Graphics) {
                            g.color = Color.LIGHT_GRAY
                            g.fillRect(0, 0, width, height)
                        }
                    }
                }
            }
        }
    }

    private fun createUI() {
        val albumSelection = AlbumSelection()
        val sidepane = SidePane(
            albumSelection = albumSelection,
            menubar = createMenuBar()
        )
        val playground = AlbumPlayground(albumSelection = albumSelection)

        sidepane.minimumSize = Dimension(200, 200)
        playground.minimumSize = Dimension(400, 400)

        splitpane = createAndAdjustSplitPane(playground, sidepane)
        contentPane = splitpane

        playground.addMouseWheelListener(playground)
        playground.addMouseMotionListener(playground)
        playground.addMouseListener(playground)
        playground.addKeyListener(playground)
        playground.isFocusable = true

        title = "Klarkson"
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1024, 1024)
        setLocationRelativeTo(null)

        addWindowListener(object : WindowAdapter() {
            override fun windowOpened(we: WindowEvent) {
                resetSplitPane()
            }
        })
    }

}

private fun createAndShowGUI() {
    val frame = KlarksonFrame()
    frame.isVisible = true
}


fun main() {
    EventQueue.invokeLater(::createAndShowGUI)
}