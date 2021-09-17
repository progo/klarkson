package klarksonmainframe

import java.awt.Color
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI
import kotlin.system.exitProcess

/**
 * Build a Swing Timer that runs after [delayMs] ms and runs the function body.
 */
fun swingDelay(delayMs: Int, func: () -> Unit) {
    Timer(delayMs) { _ -> func() } . apply {
        isRepeats = false
        start()
    }
}
// Kotlin has some trouble with the types and override if we should use both forms
// fun swingDelay(delayMs: Int, func: () -> Unit) = swingDelay(delayMs) { _ -> func() }


class KlarksonFrame : JFrame() {
    private lateinit var splitpane : JSplitPane
    private val DEFAULT_SPLIT_LOCATION = 200

    private fun getResource(s: String) = this.javaClass.classLoader.getResource(s)

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
            file.icon = ImageIcon(getResource("icons16/basketball [#790].png"))

            val eMenuItem = JMenuItem("Exit")
            eMenuItem.mnemonic = KeyEvent.VK_X
            eMenuItem.toolTipText = "Exit application"
            eMenuItem.addActionListener { exitProcess(0) }
            eMenuItem.icon = ImageIcon(getResource("icons16/exit_full_screen [#905].png"))

            file.add(eMenuItem)
            add(file)
        }
    }

    private fun resetSplitPane() {
        // Add a little delay as a hack. Probably due to using a tiling WM that
        // will immediately resize the newly created frame to its own liking.
        swingDelay(10) {
            splitpane.apply {
                dividerLocation = size.width - DEFAULT_SPLIT_LOCATION - 3
            }
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