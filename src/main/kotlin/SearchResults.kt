package klarksonmainframe

class SearchResults(result : Iterable<AlbumCover>) : Iterable<AlbumCover> {
    private val covers = result.toList()
    var seekIndex = 0
        private set

    override fun iterator(): Iterator<AlbumCover> {
        return covers.iterator()
    }

    val size get() = covers.size

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
