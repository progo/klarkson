package klarksonmainframe

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}


interface AlbumStoreEventHandler {
    /**
     * New album was added to store.
     */
    fun newAlbum(album: Album)
    // fun newAlbum(albums: Collection<Album>)

    /**
     * Sync from MPD to Store starts.
     */
    fun syncStarts()

    /**
     * Sync from MPD to Store ends.
     */
    fun syncEnds()
}

/**
 *  Layer where we keep and maintain albums from MPD or database or whereever.
 *  TODO: actually maybe not necessary. AlbumOrganizer after all knows the situation.
 */
object AlbumStore {
    private val listeners : MutableList<AlbumStoreEventHandler> = ArrayList()

    /**
     * Fetch new content from MPD.
     * This is done concurrently and suitable listeners are being in the loop.
     */
    fun fetchNewAlbumsAsync() {
        MainScope().launch {
            listenerCallback { it.syncStarts() }
            yield()

            MpdServer.produceAlbums().consumeEach { album ->
                logger.debug { "<- received an album." }
                listenerCallback { it.newAlbum(album) }
                yield()
                delay(5)
            }
            listenerCallback { it.syncEnds() }
        }
    }

    fun registerChangeListener(aseh : AlbumStoreEventHandler) {
        listeners.add(aseh)
    }

    private fun listenerCallback(block : (AlbumStoreEventHandler) -> Unit) {
        listeners.forEach {  listener ->
            listener.apply(block)
        }
    }
}

