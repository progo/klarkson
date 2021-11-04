package klarksonmainframe

import java.awt.Color
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI
import kotlin.system.exitProcess

object Resource {
    fun get(s: String) : URL = this.javaClass.classLoader.getResource(s)!!
}

class KlarksonFrame : JFrame() {
    private lateinit var splitpane : JSplitPane
    private val DEFAULT_SPLIT_LOCATION = 200

    init {
        createUI()
    }

    private fun createToolbar(): JToolBar {
        return JToolBar().apply {
            isFloatable = false

            add(JButton().apply {
                icon = ImageIcon(Resource.get("gf24/playback_prev.png"))
            })

            add(JButton().apply {
                icon = ImageIcon(Resource.get("gf24/playback_play.png"))
                toolTipText = "Play album"
                addActionListener {
                    MpdServer.addTracks(AlbumSelection.flatMap { ac : AlbumCover -> ac.album.songs },
                        play = true)
                }
            })

            add(JButton().apply {
                icon = ImageIcon(Resource.get("gf24/playback_play_plus.png"))
                toolTipText = "Add album to playlist"
                addActionListener {
                    MpdServer.addTracks(AlbumSelection.flatMap { ac : AlbumCover -> ac.album.songs })
                }
            })

            add(JButton().apply {
                icon = ImageIcon(Resource.get("gf24/playback_next.png"))
            })

            add(JButton().apply {
                icon = ImageIcon(Resource.get("gf24/playback_stop.png"))
            })
        }
    }

    private fun createMenuBar(): JMenuBar {
        return JMenuBar().apply {
            background = Color.ORANGE
            foreground = Color.WHITE
            isOpaque = true

            add(JMenu("Klarkson").apply {
                mnemonic = KeyEvent.VK_K
                icon = ImageIcon(Resource.get("gf16/burst.png"))

                add(JMenuItem("Exit").apply {
                    mnemonic = KeyEvent.VK_X
                    toolTipText = "Exit application"
                    addActionListener { exitProcess(0) }
                })
            })


            add(JMenu("Cover").apply {
                mnemonic = KeyEvent.VK_C
                icon = ImageIcon(Resource.get("gf16/picture.png"))

                add(JMenuItem("By URL...").apply {
                    mnemonic = KeyEvent.VK_U
                    addActionListener {
                        val uri = JOptionPane.showInputDialog(
                            this@KlarksonFrame,
                            "Paste URL to a cover image.",
                            "Cover URL",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            ""
                        ) as Uri

                        if (uri == "") return@addActionListener

                        val albcover = AlbumSelection.firstOrNull() ?: return@addActionListener
                        albcover.setCoverImageAsync(uri)
                    }
                })

                add(JMenuItem("Select file...").apply {
                    mnemonic = KeyEvent.VK_F
                    addActionListener {
                        val albcover = AlbumSelection.firstOrNull() ?: return@addActionListener

                        val c = JFileChooser().apply {
                            val imageFilter = FileNameExtensionFilter("Image files", *ImageIO.getReaderFileSuffixes())
                            addChoosableFileFilter(imageFilter)
                            isAcceptAllFileFilterUsed = false
                        }
                        val ret = c.showOpenDialog(this@KlarksonFrame)

                        if (ret == JFileChooser.APPROVE_OPTION) {
                            val chosenFile = c.selectedFile
                            albcover.setCoverImageAsync(chosenFile)
                        }
                    }
                })
            })

        }
    }

    private fun resetSplitPane() {
        // Add a little delay as a hack. Probably due to using a tiling WM that
        // will immediately resize the newly created frame to its own liking.
        swingDelay(10) {
            splitpane.apply {
                dividerLocation = size.width - DEFAULT_SPLIT_LOCATION - 3
                revalidate()
                repaint()
            }
        }
    }

    private fun createAndAdjustSplitPane(left: JComponent, right: JComponent): JSplitPane {
        return JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right).apply {
            dividerSize = 8

            ui = object : BasicSplitPaneUI() {
                override fun createDefaultDivider(): BasicSplitPaneDivider {
                    return object : BasicSplitPaneDivider(this) {
                        override fun paint(g: Graphics) {
                            g.color = Color.DARK_GRAY
                            g.fillRect(0, 0, width, height)
                        }
                    }
                }
            }
        }
    }

    private fun createUI() {
        val albumOrg = AlbumOrganizer()
        val sidepane = SidePane(
            menubar = createMenuBar(),
            toolbar = createToolbar(),
            albums = albumOrg
        )
        val playground = AlbumPlayground(albums = albumOrg)

        sidepane.minimumSize = Dimension(200, 200)
        playground.minimumSize = Dimension(400, 400)

        splitpane = createAndAdjustSplitPane(playground, sidepane)
        contentPane = splitpane

        playground.addMouseWheelListener(playground)
        playground.addMouseMotionListener(playground)
        playground.addMouseListener(playground)
        playground.addKeyListener(playground)
        playground.isFocusable = true
        playground.isRequestFocusEnabled = true

        title = "Klarkson"
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1024, 1024)
        setLocationRelativeTo(null)

        addWindowListener(object : WindowAdapter() {
            override fun windowOpened(we: WindowEvent) {
                resetSplitPane()
            }

            override fun windowClosing(we: WindowEvent) {
                super.windowClosed(we)
                printFinalStatistics()
            }
        })
    }
}

fun printFinalStatistics() {
    println("Bye!")
    println("----------------------------------------------------------")
    println(LastFmClient.getStatistics())
    println("----------------------------------------------------------")
    println(AlbumCoverImageService.reportCacheStatistics())
}

lateinit var klarksonFrame : KlarksonFrame

private fun createAndShowGUI() {
    klarksonFrame = KlarksonFrame()
    klarksonFrame.isVisible = true
}

fun main() {
    EventQueue.invokeLater(::createAndShowGUI)
}