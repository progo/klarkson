package klarksonmainframe

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}


/**
 * Sync or load event type, ie the source of incoming albums.
 */
enum class SyncEventType  {
    MPD,
    Database
}


interface AlbumStoreEventHandler {
    /**
     * New album was added to store.
     */
    fun newAlbum(album: Album)
    // fun newAlbum(albums: Collection<Album>)

    /**
     * Sync from, for example MPD, to Store starts.
     * [type] will tell you more about it.
     */
    fun syncStarts(type: SyncEventType)

    /**
     * Sync events, no more albums incoming.
     */
    fun syncEnds()
}

/**
 *  Layer where we keep and maintain albums from MPD or database or whereever.
 *  TODO: actually maybe not necessary. AlbumOrganizer after all knows the situation.
 */
object AlbumStore {
    private val listeners : MutableList<AlbumStoreEventHandler> = ArrayList()
    // private val knownSongs: MutableSet<Song> = HashSet()
    private val knownFiles: MutableSet<String> = HashSet()

    /**
     * Fetch new content from MPD.
     * This is done concurrently and suitable listeners are being in the loop.
     */
    fun fetchNewAlbumsAsync(query: String? = null) {
        MainScope().launch {
            listenerCallback { it.syncStarts(SyncEventType.MPD) }
            yield()

            MpdServer.produceAlbums(query).consumeEach { album ->
                logger.debug { "<- received an album." }
                storeAlbum(album)
                listenerCallback { it.newAlbum(album) }
                yield()
                delay(1)
            }
            listenerCallback { it.syncEnds() }
            logger.debug { "AlbumStore knows ${knownFiles.size} files."}
        }
    }

    fun put(albs: Iterable<Album>) {
        listenerCallback { it.syncStarts(SyncEventType.Database) }
        albs.forEach { a ->
            listenerCallback { it.newAlbum(a) }
        }
        listenerCallback { it.syncEnds() }
    }

    /** Do we know this file already? */
    fun knowFile(f: String): Boolean {
        return f in knownFiles
    }

    /**
     * Load from DB and populate inbox
     */
    fun load() {
        val albums = Persist.loadInbox()
        albums.forEach { alb ->
            listenerCallback { it.newAlbum(alb) }
        }
    }

    /**
     * Process a newly gathered Album [a] somehow.
     */
    private fun storeAlbum(a: Album) {
        knownFiles.addAll(a.songs.map { it.file })
        Persist.persist(a, x=null, y=null)
    }

    ///// Event handler things
    fun registerChangeListener(aseh : AlbumStoreEventHandler) {
        listeners.add(aseh)
    }

    private fun listenerCallback(block : (AlbumStoreEventHandler) -> Unit) {
        listeners.forEach {  listener ->
            listener.apply(block)
        }
    }
}

