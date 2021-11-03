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

    fun deleteSelected() {
        val lc = listComp ?: return
        val lm : DefaultListModel<AlbumCover> = lc.model as DefaultListModel<AlbumCover>
        for (index in lc.selectedIndices.reversed()) {
            lm.remove(index)
        }
    }

}