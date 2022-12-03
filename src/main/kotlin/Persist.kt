package klarksonmainframe

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection


object DBAlbum : Table() {
    val id = integer("id").autoIncrement()
    val artist = varchar("artist", length=256).index()
    val album = varchar("album", length=256).index()
    val year = integer("year").nullable()
    val discCount = integer("discCount").nullable()
    val runtime = integer("runtime").index()
    override val primaryKey  = PrimaryKey(id)

    // when x,y are defined, album is on the playground.
    // our code checks for x
    val x = integer("x").nullable().index()
    val y = integer("y").nullable()
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
            SchemaUtils.create(DBAlbum, DBTrack)
        }
    }

    /**
     * Load albums from database to playground via AlbumOrganizer.
     */
    fun load(amo : AlbumOrganizer) {
        transaction {
            val albums = DBAlbum.select { DBAlbum.x neq null }

            albums.forEach {
                val songs = DBTrack
                    .select { DBTrack.albumId eq it[DBAlbum.id] }
                    .map { r -> Song.make(r, it[DBAlbum.album]) }

                val alb = Album.make(it, songs)
                val x = it[DBAlbum.x] as Int
                val y = it[DBAlbum.y] as Int

                val ac = AlbumCover(alb, x, y)
                amo.put(ac)
            }
        }
    }

    fun loadInbox(): Collection<Album> {
        val albums = ArrayList<Album>()
        transaction {
            val albs = DBAlbum.select { DBAlbum.x eq null }
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
    fun persist(ac: AlbumCover): Int {
        return persist(ac.album, ac.x, ac.y)
    }

    fun persist(a: Album, x: Int?, y: Int?): Int {
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
                    it[DBAlbum.x] = x
                    it[DBAlbum.y] = y
                }
            }
            else {
                albumID = DBAlbum.insert {
                    it[artist] = a.artist
                    it[DBAlbum.x] = x
                    it[DBAlbum.y] = y
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
    fun persist(acs : Iterable<AlbumCover>) {
        transaction {
            acs.forEach { ac ->
                persist(ac)
            }
        }
    }
}
