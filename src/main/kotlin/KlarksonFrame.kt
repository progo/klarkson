package klarksonmainframe

import java.awt.Color
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URL
import javax.swing.*
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
                    MpdServer.addAlbums(AlbumSelection, play = true)
                }
            })

            add(JButton().apply {
                icon = ImageIcon(Resource.get("gf24/playback_play_plus.png"))
                toolTipText = "Add album to playlist"
                addActionListener {
                    MpdServer.addAlbums(AlbumSelection)
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

    private fun createMenuBar(amo : AlbumOrganizer): JMenuBar {
        return JMenuBar().apply {
            background = Color.ORANGE
            foreground = Color.WHITE
            isOpaque = true

            add(JMenu("Klarkson").apply {
                mnemonic = KeyEvent.VK_K
                icon = ImageIcon(Resource.get("gf16/burst.png"))

                add(JMenuItem("Check new").apply {
                    mnemonic = KeyEvent.VK_C
                    toolTipText = "Check new albums from MPD"
                    addActionListener { AlbumStore.fetchNewAlbumsAsync() }
                })

                add(JMenuItem("Check integrity").apply {
                    mnemonic = KeyEvent.VK_I
                    toolTipText = "Check integrity of album files: metadata and file changes."
                    addActionListener { amo.checkIntegrity() }
                })

                add(JMenuItem("Exit").apply {
                    accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK)
                    mnemonic = KeyEvent.VK_X
                    toolTipText = "Exit application"
                    addActionListener { this@KlarksonFrame.dispose() }
                })
            })

            add(JMenu("Selection").apply {
                mnemonic = KeyEvent.VK_S
                icon = ImageIcon(Resource.get("gf16/3x3_grid.png"))

                add(JMenuItem("Delete selected...").apply {
                })
            })

            add(JMenu("Cover").apply {
                mnemonic = KeyEvent.VK_C
                icon = ImageIcon(Resource.get("gf16/picture.png"))

                add(JMenuItem("Make a variation of existing cover").apply {
                    mnemonic = KeyEvent.VK_M
                    addActionListener(copyExistingCover)
                })

                add(JMenuItem("Search server for cover...").apply {
                    toolTipText = "Search for covers with different search terms."
                    addActionListener(askCoverServer)
                })

                add(JMenuItem("Paste from Clipboard").apply {
                    accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK)
                    addActionListener(setCoverFromClipboard)
                })

                add(JMenuItem("By URL...").apply {
                    mnemonic = KeyEvent.VK_U
                    addActionListener(setCoverFromURL)
                })

                add(JMenuItem("Select file...").apply {
                    mnemonic = KeyEvent.VK_F
                    addActionListener(setCoverFromFile)
                })
            })

            add(JMenuItem("Save!").apply {
                addActionListener {
                    amo.save()
                }
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
        initActions(rootPane)

        val albumOrg = AlbumOrganizer()
        val sidepane = SidePane(
            menubar = createMenuBar(albumOrg),
            toolbar = createToolbar(),
            albums = albumOrg
        )
        val playground = AlbumPlayground(albums = albumOrg)

        // TODO make sure we run this on the side
        albumOrg.load()

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
                super.windowClosing(we)
                printFinalStatistics()
            }

            override fun windowClosed(we: WindowEvent) {
                super.windowClosed(we)
                printFinalStatistics()
                exitProcess(0)
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
    Persist.initializeDatabase()
    EventQueue.invokeLater(::createAndShowGUI)
}