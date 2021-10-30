package klarksonmainframe

import java.awt.Color
import java.awt.Font
import javax.swing.JTextField

class SearchBox : JTextField() {
    init {
        foreground = Color.ORANGE
        background = Color.BLACK
        font = Font("sans serif", Font.PLAIN, 16)
        text = "Search..."

        SearchBoxActivator.initSearchBox(this)
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