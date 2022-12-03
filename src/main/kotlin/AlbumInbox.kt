package klarksonmainframe

import javax.swing.*

// dirty hack to pass global STATE
object AlbumInboxSelection {
    private var listComp : JList<AlbumCover>? = null

    fun setListComp(lc : JList<AlbumCover>) {
        listComp = lc
    }

    fun getSelection() : List<AlbumCover> {
        val lc = listComp ?: return emptyList()
        return lc.selectedValuesList.toList()
    }

    /**
     * A number of covers have shifted from inbox to playground at this point.
     */
    fun acceptDragNDrop() {
        val lc = listComp ?: return
        val lm : DefaultListModel<AlbumCover> = lc.model as DefaultListModel<AlbumCover>
        for (index in lc.selectedIndices.reversed()) {
            val album = lm.get(index).album
            Persist.persist(album, inInbox = false)
            lm.remove(index)
        }
    }

}