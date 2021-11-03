package klarksonmainframe

import java.awt.Color
import java.awt.Font
import java.awt.event.*
import javax.swing.JTextField

class SearchBox : JTextField() {
    private val backgroundFocus = Color.BLACK
    private val backgroundDefocus = Color.GRAY
    private val flashColorBg = Color.YELLOW
    private val flashColorFg = Color.BLACK
    private val foregroundColor = Color.ORANGE

    init {
        foreground = foregroundColor
        background = backgroundDefocus

        font = Font("sans serif", Font.PLAIN, 16)
        text = "Search..."

        SearchBoxActivator.initSearchBox(this)

        addFocusListener(object : FocusAdapter() {
            override fun focusGained(p0: FocusEvent?) {
                background = flashColorBg
                foreground = flashColorFg
                repaint()

                swingDelay(100) {
                    foreground = foregroundColor
                    background = backgroundFocus
                    repaint()
                }
            }

            override fun focusLost(p0: FocusEvent?) {
                background = backgroundDefocus
                repaint()
            }
        })

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(ke: KeyEvent) {
                if (ke.isControlDown && ke.keyCode == KeyEvent.VK_F) {
                    focus()
                }
            }
        })
    }

    fun focus() {
        requestFocus()
        selectAll()
    }
}


object SearchBoxActivator {
    private lateinit var searchBox: SearchBox

    fun initSearchBox(sb: SearchBox) { searchBox = sb }
    fun focus() {
        searchBox.focus()
    }
}