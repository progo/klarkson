package klarksonmainframe

import org.jetbrains.exposed.sql.*
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

object DBAlbum : Table() {
    val id = integer("id").autoIncrement()
    val artist = varchar("artist", length=256).index()
    val album = varchar("album", length=256).index()
    val year = integer("year").nullable()
    val discCount = integer("discCount").nullable()
    val runtime = integer("runtime").index()
    override val primaryKey  = PrimaryKey(id)
}

object DBTrack : Table() {
    val id = integer("id").autoIncrement()
    val artist = varchar("artist", length = 256)
    // val album = varchar("album", length = 256)
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
            SchemaUtils.create(DBVersion, DBAlbum, DBAlbumCover, DBTrack)
        }
    }

    /**
     * Load albums from database to an AlbumOrganizer.
     */
    fun load(amo : AlbumOrganizer) {
        transaction {
            val latest = DBVersion
                .slice(DBVersion.id)
                .selectAll()
                .limit(1)
                .orderBy(DBVersion.created to SortOrder.DESC)
                .firstOrNull()
                ?.get(DBVersion.id) ?: return@transaction

            println("Latest persisted version is v$latest")

            val albums = (DBAlbum innerJoin DBAlbumCover)
                .select { DBAlbumCover.versionId eq latest }

            println("Loading ${albums.count()} records...")

            albums.forEach {
                val songs = DBTrack
                    .select { DBTrack.albumId eq it[DBAlbum.id] }
                    .map { r -> Song.make(r, it[DBAlbum.album]) }

                val alb = Album.make(it, songs)
                val x = it[DBAlbumCover.x]
                val y = it[DBAlbumCover.y]
                println("- ($x, $y) $alb")

                val ac = AlbumCover(alb, x, y)
                amo.put(ac)
            }
        }
    }

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
                        it[albumArtist] = s.albumArtist ?: ""
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
