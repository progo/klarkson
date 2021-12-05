package klarksonmainframe

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection


private object DBVersion : Table() {
    val id = integer("id").autoIncrement()
    val created = datetime("created").defaultExpression(CurrentDateTime())
    override val primaryKey  = PrimaryKey(id)
}

private object DBAlbumCover : Table() {
    val id = integer("id").autoIncrement()
    val x  = integer("x").index()
    val y  = integer("y").index()
    val versionId = (integer("versionId") references DBVersion.id).index()
    val albumId = (integer("albumId") references DBAlbum.id).uniqueIndex()

    override val primaryKey  = PrimaryKey(id)
}

private object DBAlbum : Table() {
    val id = integer("id").autoIncrement()
    val artist = varchar("artist", length=256).index()
    val album = varchar("album", length=256).index()
    val year = integer("year").nullable()
    val discCount = integer("discCount").nullable()
    val runtime = integer("runtime").index()
    override val primaryKey  = PrimaryKey(id)
}

private object DBTrack : Table() {
    val id = integer("id").autoIncrement()
    val artist = varchar("artist", length = 256)
    // val album = varchar("album", length = 256)
    val albumId = integer("albumId") references DBAlbum.id
    val title = varchar("title", length = 256).index()
    val file = varchar("file", length = 512).index()
    // val albumArtist = varchar("albumArtist", length = 256)
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
            SchemaUtils.create(DBVersion, DBAlbum, DBAlbumCover, DBTrack)
        }
    }

    // fun load() : Iterable<AlbumCover> { }

    fun persist(acs : Iterable<AlbumCover>) {
        transaction {
            val version = DBVersion.insert {  } get DBVersion.id

            acs.forEach { albumcover ->

                val album_id = DBAlbum.insert {
                    it[artist] = albumcover.album.artist
                    it[album] = albumcover.album.album
                    it[year] = albumcover.album.year
                    it[discCount] = albumcover.album.discCount
                    it[runtime] = albumcover.album.runtime
                } get DBAlbum.id

                DBAlbumCover.insert {
                    it[x] = albumcover.x
                    it[y] = albumcover.y
                    it[albumId] = album_id
                    it[versionId] = version
                }

                albumcover.album.songs.forEach { s ->
                    DBTrack.insert {
                        it[artist] = s.artist
                        it[albumId] = album_id
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
    }
}
