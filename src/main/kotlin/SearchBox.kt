package klarksonmainframe

import java.awt.Color
import java.awt.Font
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTextField

class SearchBox : JTextField() {
    private val backgroundFocus = Color.BLACK
    private val backgroundDefocus = Color.GRAY

    init {
        foreground = Color.ORANGE
        background = backgroundDefocus

        font = Font("sans serif", Font.PLAIN, 16)
        text = "Search..."

        SearchBoxActivator.initSearchBox(this)

        addFocusListener(object : FocusAdapter() {
            override fun focusGained(p0: FocusEvent?) {
                background = backgroundFocus
                repaint()
            }

            override fun focusLost(p0: FocusEvent?) {
                background = backgroundDefocus
                repaint()
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