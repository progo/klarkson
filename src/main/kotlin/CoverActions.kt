package klarksonmainframe

import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

val setCoverFromClipboard = object : AbstractAction() {
    override fun actionPerformed(p0: ActionEvent?) {
        val albumcover = AlbumSelection.firstOrNull() ?: return
        albumcover.setCoverImageFromClipboard()
    }
}

val copyExistingCover = object : AbstractAction() {
    override fun actionPerformed(p0: ActionEvent?) {
        if (AlbumSelection.size() > 1) {
            println("Might get hairy with multiple selection.")
            return
        }

        val albumcoverTarget = AlbumSelection.firstOrNull() ?: return
        val popup = showMessage("Select a cover to vary from.", timeMillis=5000000)

        AlbumSelection.registerOnetimeListener {
            // a new selection has been made
            if (AlbumSelection.size() == 1) {
                val albumCoverSource = AlbumSelection.first()
                // println("Copying cover from $albumCoverSource to $albumcoverTarget...!")
                albumcoverTarget.makeVariantCoverFrom(albumCoverSource)
                popup.hide()
                // Restore original selection
                AlbumSelection.replace(setOf(albumcoverTarget))
            }
        }
    }
}

val askCoverServer = object : AbstractAction() {
    override fun actionPerformed(p0: ActionEvent?) {
        val cover = AlbumSelection.firstOrNull() ?: return
        val aad = ArtistAlbumDialog(
            klarksonFrame,
            cover.album.artist,
            cover.album.album
        )
        aad.isVisible = true

        if (aad.closedOkay) {
            println("redo search with [${aad.artist}] - [${aad.album}]...")
            cover.setCoverImageAlternativeAsync(aad.artist, aad.album)
        }
    }
}

val setCoverFromURL = object : AbstractAction() {
    override fun actionPerformed(p0: ActionEvent?) {
        val uri = JOptionPane.showInputDialog(
            klarksonFrame,
            "Paste URL to a cover image.",
            "Cover URL",
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            ""
        ) as Uri

        if (uri == "") return

        val albcover = AlbumSelection.firstOrNull() ?: return
        albcover.setCoverImageAsync(uri)
    }
}

val setCoverFromFile = object : AbstractAction() {
    override fun actionPerformed(p0: ActionEvent?) {
        val albcover = AlbumSelection.firstOrNull() ?: return

        val c = JFileChooser().apply {
            val imageFilter = FileNameExtensionFilter("Image files", *ImageIO.getReaderFileSuffixes())
            addChoosableFileFilter(imageFilter)
            isAcceptAllFileFilterUsed = false
        }
        val ret = c.showOpenDialog(klarksonFrame)

        if (ret == JFileChooser.APPROVE_OPTION) {
            val chosenFile = c.selectedFile
            albcover.setCoverImageAsync(chosenFile)
        }
    }
}

// Add global key bindings to the actions where it matters
fun initActions(c : JComponent) {
    val map = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK), "setCoverFromClipboard")
    c.actionMap.put("setCoverFromClipboard", setCoverFromClipboard)
}