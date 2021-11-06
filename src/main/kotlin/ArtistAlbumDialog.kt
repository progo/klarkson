package klarksonmainframe

import java.awt.Frame
import javax.swing.*

private const val TITLE = "Artist and album"

class ArtistAlbumDialog(
    win: Frame,
    prefilledArtist: String,
    prefilledAlbum: String
): JDialog(win, TITLE, ModalityType.APPLICATION_MODAL) {
    var closedOkay : Boolean = false
        private set


    private val txtArtist = JTextField(prefilledArtist)
    private val txtAlbum = JTextField(prefilledAlbum)

    val artist : String get() = txtArtist.text.trim()
    val album : String get() = txtAlbum.text.trim()

    init {
        setSize(350, 250)
        setLocationRelativeTo(win)

        contentPane = JPanel() .apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
            add(JLabel("Artist"))
            add(txtArtist)

            add(JLabel("Album"))
            add(txtAlbum)

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(JButton("Search").apply {
                    addActionListener {
                        closedOkay = true
                        this@ArtistAlbumDialog.isVisible = false
                    }
                })
                add(JButton("Cancel").apply {
                    addActionListener {
                        closedOkay = false
                        this@ArtistAlbumDialog.isVisible = false
                    }
                })
            })
        }
        pack()
    }
}