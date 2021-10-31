package klarksonmainframe

class SearchResults(result : Iterable<AlbumCover>) : Iterable<AlbumCover> {
    private val covers = result.toList()
    private var seekIndex = 0

    override fun iterator(): Iterator<AlbumCover> {
        return covers.iterator()
    }

    fun size() = covers.size

    fun next(cycle: Boolean = false) : AlbumCover? {
        if (covers.isEmpty()) {
            return null
        }

        if (cycle && seekIndex >= covers.size) {
            seekIndex = 0
        }

        if (seekIndex < covers.size) {
            val ret = covers[seekIndex]
            println("Result ${1+seekIndex}/${covers.size}")
            seekIndex++
            return ret
        }
        else {
            return null
        }
    }
}
