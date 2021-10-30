package klarksonmainframe

import kotlin.math.abs

/**
 * AlbumOrganizer is our data structure and collector for albums in the playground.
 */
class AlbumOrganizer : Iterable<AlbumCover> {
    // TreeSet is a strong idea here
    private val albums : ArrayList<AlbumCover> = ArrayList()

    fun put(a: AlbumCover) {
        albums.add(a)
    }

    fun size() = albums.size

    /**
     *  Get the topmost cover around a virtual point, or null.
     */
    fun getByLocation(x: Int, y: Int): AlbumCover? {
        fun pred(a: AlbumCover) : Boolean {
            return ((a.x - ALBUM_COVER_SIZE /2 < x) and (x < a.x + ALBUM_COVER_SIZE /2)
                    and (a.y - ALBUM_COVER_SIZE /2 < y) and (y < a.y + ALBUM_COVER_SIZE /2))
        }
        return albums.lastOrNull(::pred)
    }

    fun getByLocation(p: Point) = getByLocation(p.x, p.y)

    /**
     * Points a and b form a rectangular region: find all albums within the region.
     */
    fun allAlbumsWithinRegion(a: Point, b: Point) : List<AlbumCover> {
        val (x1, y1) = a
        val (x2, y2) = b
        fun p(a: AlbumCover) : Boolean = (x1 > a.x) && (a.x > x2) && (y1 > a.y) && (a.y > y2)
        return albums.filter(::p)
    }

    /**
     * Find a nearby to point [p] an album cover that's to direction [dir].
     */
    fun getAlbumInTheDirectionOf(p: Point, dir: Direction) : AlbumCover? {
        /*
        First let's see if a simple conefied approach works okay for us:

              \             /
               \    UP     /
                \         /
                 \       /
                  \     /
            LEFT   (x,y)   RIGHT
                  /     \
                 /       \
                /         \
               /   DOWN    \
              /             \

         Findings: this works quite okay. Works well for matrix-aligned stuff.
         */

        // TODO actually a parabel search radius will be a smarter approach once
        // we implement something.

        // TODO Linear search and ordering over everything is not fast.
        // Maybe best to incorporate a distance limitation right in the filtering
        // pred.

        val pred : (AlbumCover) -> Boolean = when (dir) {
            Direction.UP -> { ac -> (ac.y > p.y) and (abs(ac.x - p.x) < (ac.y - p.y)*2.0) }
            Direction.DOWN -> { ac -> (ac.y < p.y) and (abs(ac.x - p.x) < (p.y - ac.y)*2.0) }
            Direction.LEFT -> { ac -> (ac.x > p.x) and (abs(ac.y - p.y) < (ac.x - p.x)*2.0) }
            Direction.RIGHT -> { ac -> (ac.x < p.x) and (abs(ac.y - p.y) < (p.x - ac.x)*2.0) }
        }

        return albums.filter(pred).minByOrNull { p.distance(it.x, it.y) }
    }

    /**
     * Seq of everything
     */
    override operator fun iterator() : Iterator<AlbumCover> {
        return albums.iterator()
    }

    /**
     *  Sort and order the albums. Subject to become a noop if we switch to a tree structure.
     */
    fun reorganize() {
        albums.sort()
    }


    /**
     * Search utilities...
     */
    fun searchAlbums(album: String = "", artist: String = "", runtime: Int = 0) : Iterable<AlbumCover> {
        val res = albums.filter { albumCover -> albumCover.album.artist == artist }
        return res
    }
}