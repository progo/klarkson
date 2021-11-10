package klarksonmainframe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing

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
            MpdServer.produceAlbums().consumeEach { album ->
                listenerCallback { it.newAlbum(album) }
                kotlinx.coroutines.delay(10)
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

