package klarksonmainframe

import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke

val setCoverFromClipboard = object : AbstractAction("Get Album cover from Clipboard") {
    override fun actionPerformed(p0: ActionEvent?) {
        val albumcover = AlbumSelection.firstOrNull() ?: return
        albumcover.setCoverImageFromClipboard()
    }
}

fun initActions(c : JComponent) {
    val map = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK), "setCoverFromClipboard")
    c.actionMap.put("setCoverFromClipboard", setCoverFromClipboard)
}