package klarksonmainframe

import mu.KotlinLogging
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

interface SearchEventHandler {
    /**
     * Search has commenced with results.
     **/
    fun newSearch(results: SearchResults)
    /**
     * Search event fires, but it's the same query, same results as before.
     **/
    fun nextResult()
}


/**
 * Fires for pre/post save and load events.
 */
interface SaveLoadEventHandler {
    fun beforeSave()
    fun afterSave()
    fun beforeLoad()
    fun afterLoad()
}


/**
 * AlbumOrganizer (AMO) is our data structure and collector for albums
 * in the playground.
 *
 * We will need to figure if this has to be coupled with Playground,
 * or can we reuse this for Inbox.
 */
class AlbumOrganizer : Iterable<AlbumCover> {
    // TreeSet is a strong idea here
    private val albums : ArrayList<AlbumCover> = ArrayList()

    // Search state
    private var previousSearch : String = " "
    private val searchListeners = ArrayList<SearchEventHandler>()
    private val saveLoadListeners = ArrayList<SaveLoadEventHandler>()

    /**
     * Save to db.
     */
    fun save() {
        saveLoadListenerCallback { it.beforeSave() }
        Persist.persist(this)
        saveLoadListenerCallback { it.afterSave() }
    }

    /**
     * Load from db.
     */
    fun load() {
        saveLoadListenerCallback { it.beforeLoad() }
        Persist.load(this)
        saveLoadListenerCallback { it.afterLoad() }
    }


    /**
     * Remove from Playground (and bring them back to inbox)
     */
    fun removeAlbums(albums2go: Iterable<AlbumCover>) {
        albums2go.forEach {
            albums.remove(it)
            Persist.persist(it)
        }
        AlbumStore.put(albums2go.map { it.album })
    }

    /**
     * Integrity check means that we go over the files and see if anything has changed.
     *
     * Deleted files, moved files, and retagged files.
     */
    fun checkIntegrity() {
        val faultyAlbums = mutableSetOf<AlbumCover>()
        val emptyAlbums = mutableSetOf<AlbumCover>()

        for (ac in albums) {
            val album = ac.album
            logger.debug { "Checking integrity of ${album.album}..." }

            if (album.songs.isEmpty()) {
                emptyAlbums.add(ac)
                continue
            }

            album.songs.forEach { song ->
                when (MpdServer.checkFile(song)) {
                    FileStatus.MISSING -> { faultyAlbums.add(ac) ; return@forEach }
                    FileStatus.TAGS -> { faultyAlbums.add(ac) ; return@forEach }
                    FileStatus.OK -> Unit
                }
            }
        }

        logger.debug { "Empty albums (bad) = ${emptyAlbums.size}" }
        logger.debug { "Faulty albums (reconstruct) = ${faultyAlbums.size}" }

        // A faulty album may for example divide a seq of songs in two, so when
        // we deal with faulty albums, we have to figure a way to reconstruct
        // related "okay" albums too.
        faultyAlbums.forEach {
            println("- ${it.album}")
        }

        val faultySongs = faultyAlbums.flatMap {  ac -> ac.album.songs }

        // this will result nothing if there are several albums
        val dir = faultySongs
            .map { s -> s.file }
            .reduce { p1, p2 -> p1.commonPrefixWith(p2) }

        logger.debug { "We can redo `$dir` maybe." }

    }

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
     * Listener code for save/load events
     */

    fun registerSaveLoadListener(sleh : SaveLoadEventHandler) {
        saveLoadListeners.add(sleh)
    }

    private fun saveLoadListenerCallback(block : (SaveLoadEventHandler) -> Unit) {
        saveLoadListeners.forEach { listener ->
            listener.apply(block)
        }
    }

    /**
     * Listeners for search events
     */
    fun registerSearchEventListener(seh : SearchEventHandler) {
        searchListeners.add(seh)
    }
    private fun notifySearchEventListeners(results: SearchResults? = null, new : Boolean) {
        searchListeners.forEach {
            if (new && results != null)
                it.newSearch(SearchResults(results))
            if (new && results == null)
                throw KotlinNullPointerException("what the hell's going on?")
            if (!new)
                it.nextResult()
        }
    }

    /**
     * Search utilities...
     * And yes, we are dealing with entire albums here.
     * If a track name should match, the whole album will be included.
     */

    fun startOrContinueSearch(query : String) {
        val q = query.trim()

        // Continue previously made search
        if (q == previousSearch) {
            notifySearchEventListeners(new=false)
        }

        // ...or  start a new one.
        else {
            val sr = SearchResults(searchAlbums(parseQuery(q)))
            println("Start a new search with [$q] => ${sr.size} results.")
            previousSearch = q
            notifySearchEventListeners(results = sr, new=true)
        }
    }

    private fun searchAlbums(query: ParsedSearchQuery) : Iterable<AlbumCover> {
        val result = TreeSet<AlbumCover>()
        val unionp = query.matchMode == SearchMode.MATCH_ANY

        if (query.artist != null) {
            // println("Searching by artist [${query.artist}]")
            result.addAll(albums.filter { ac -> query.artist.matches(ac.album.artist) })
        }

        if (query.album != null) {
            // println("Searching by album [${query.album}]")
            val pred = { ac : AlbumCover -> query.album.matches(ac.album.album) }

            if (unionp) {
                result.addAll(albums.filter(pred))
            }
            else {
                result.retainAll(pred)
            }
        }

        if (query.runtime != null) {
            val pred = { ac : AlbumCover -> query.runtime.matches(ac.album) }

            if (unionp) {
                result.addAll(albums.filter(pred))
            }
            else {
                result.retainAll(pred)
            }
        }

        // println("Got ${result.size} matches.")
        return result
    }
}