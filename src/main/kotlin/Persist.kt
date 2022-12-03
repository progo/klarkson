package klarksonmainframe

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection


private object DBAlbumCover : Table() {
    val id = integer("id").autoIncrement()
    val x  = integer("x").index()
    val y  = integer("y").index()
    val albumId = (integer("albumId") references DBAlbum.id).uniqueIndex()

    override val primaryKey  = PrimaryKey(id)
}

object DBAlbum : Table() {
    val id = integer("id").autoIncrement()
    val artist = varchar("artist", length=256).index()
    val album = varchar("album", length=256).index()
    val year = integer("year").nullable()
    val discCount = integer("discCount").nullable()
    val runtime = integer("runtime").index()
    override val primaryKey  = PrimaryKey(id)

    // inboxed is:
    // true: the album is currently in inbox
    // false: the album is placed in the playground
    val inboxed = bool("inboxed").index()
}

object DBTrack : Table() {
    val id = integer("id").autoIncrement()
    val artist = varchar("artist", length = 256)
    val albumId = integer("albumId") references DBAlbum.id
    val title = varchar("title", length = 256).index()
    val file = varchar("file", length = 512).index()
    val albumArtist = varchar("albumArtist", length = 256)
    val trackNumber = integer("trackNumber").nullable()
    val discNumber = integer("discNumber").nullable()
    val year = integer("year").nullable()
    val comments = varchar("comments", length = 256).nullable()
    val genre = varchar("genre", length = 256).nullable()
    val runtime = integer("runtime")

    override val primaryKey  = PrimaryKey(id)
}


object Persist {

    fun initializeDatabase() {
        Database.connect("jdbc:sqlite:/home/progo/klarkson.db", "org.sqlite.JDBC")

        TransactionManager.manager.defaultIsolationLevel =
            Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.create(DBAlbum, DBAlbumCover, DBTrack)
        }
    }

    /**
     * Load albums from database to playground via AlbumOrganizer.
     */
    fun load(amo : AlbumOrganizer) {
        transaction {
            val albums = (DBAlbum innerJoin DBAlbumCover)
                .select { DBAlbum.inboxed eq false }

            albums.forEach {
                val songs = DBTrack
                    .select { DBTrack.albumId eq it[DBAlbum.id] }
                    .map { r -> Song.make(r, it[DBAlbum.album]) }

                val alb = Album.make(it, songs)
                val x = it[DBAlbumCover.x]
                val y = it[DBAlbumCover.y]

                val ac = AlbumCover(alb, x, y)
                amo.put(ac)
            }
        }
    }

    fun loadInbox(): Collection<Album> {
        val albums = ArrayList<Album>()
        transaction {
            val albs = DBAlbum.select { DBAlbum.inboxed eq true }
            albs.forEach {
                val songs = DBTrack
                    .select { DBTrack.albumId eq it[DBAlbum.id] }
                    .map { r -> Song.make(r, it[DBAlbum.album]) }
                val alb = Album.make(it, songs)
                albums.add(alb)
            }
        }
        return albums
    }

    /** Store one album. Returns an id. */
    fun persist(a: Album, inInbox: Boolean): Int {
        var albumID : Int = -1
        transaction {

            // See if it's already in the system.
            // TODO if misbehaving, add check for song filepath.
            val alb = DBAlbum
                .select { DBAlbum.artist eq a.artist }
                .andWhere { DBAlbum.album eq a.album }
                .andWhere { DBAlbum.year eq a.year }
                .andWhere { DBAlbum.discCount eq a.discCount }
                .andWhere { DBAlbum.runtime eq a.runtime }
                .firstOrNull()

            if (alb != null) {
                albumID = alb[DBAlbum.id]
                DBAlbum.update ({ DBAlbum.id eq albumID }) {
                    it[inboxed] = inInbox
                }
            }
            else {
                albumID = DBAlbum.insert {
                    it[artist] = a.artist
                    it[inboxed] = inInbox
                    it[album] = a.album
                    it[year] = a.year
                    it[discCount] = a.discCount
                    it[runtime] = a.runtime
                } get DBAlbum.id

                a.songs.forEach { s ->
                    DBTrack.insert {
                        it[artist] = s.artist
                        it[albumArtist] = s.albumArtist ?: ""
                        it[albumId] = albumID
                        it[title] = s.title
                        it[file] = s.file
                        it[trackNumber] = s.trackNumber
                        it[discNumber] = s.discNumber
                        it[year] = s.year
                        it[comments] = s.comment
                        it[genre] = s.genre
                        it[runtime] = s.runtime
                    }
                }
            }

        }
        return albumID
    }

    /**
     * Store iterable of albumcovers
     */
    fun persist(acs : Iterable<AlbumCover>, inInbox: Boolean) {
        transaction {
            acs.forEach { albumcover ->
                // Because we're storing playground items here, inInbox should be always false.
                val album_id = persist(albumcover.album, inInbox)

                DBAlbumCover.deleteWhere { DBAlbumCover.albumId eq album_id }

                DBAlbumCover.insert {
                    it[x] = albumcover.x
                    it[y] = albumcover.y
                    it[albumId] = album_id
                }

            }
        }
    }
}
