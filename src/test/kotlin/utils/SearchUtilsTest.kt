package utils

import klarksonmainframe.utils.parseSearch
import klarksonmainframe.utils.search
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SearchUtilsTest {
    @Test
    fun `#parseSearch with just artist`() {
        val search = "\\a pink floyd"
        val p = parseSearch(search)
        assertEquals("pink floyd", p.artist)
        assertEquals("", p.album)
    }

    @Test
    fun `#parseSearch with just album`() {
        val search = "\\b dark side of the moon"
        val p = parseSearch(search)
        assertEquals("dark side of the moon", p.album)
        assertEquals("", p.artist)
    }

    @Test
    fun `#parseSearch with just runtime`() {
        val p = parseSearch("\\l10-25 ")
        assertEquals("", p.artist)
        assertEquals("", p.album)
        assertEquals("10-25", p.runtime)
    }

    @Test
    fun `#parseSearch pink floyd over an hour`() {
        val p = parseSearch("\\aPink floyd \\l60-")
        assertEquals("Pink floyd", p.artist)
        assertEquals("", p.album)
        assertEquals("60-", p.runtime)
    }

    @Test
    fun `#parseSearch mixed search`() {
        val p = parseSearch("hasta la vista")
        assertEquals("hasta la vista", p.artist)
        assertEquals("hasta la vista", p.album)
        assertEquals("", p.runtime)
    }

    @Test
    fun `#parseSearch mixed search with runtime`() {
        val p = parseSearch("hasta la vista \\l-56")
        assertEquals("hasta la vista", p.artist)
        assertEquals("hasta la vista", p.album)
        assertEquals("-56", p.runtime)
    }

    @Test
    fun `#parseSearch empty search`() {
        val p = parseSearch("")
        assertEquals("", p.artist)
        assertEquals("", p.album)
        assertEquals("", p.runtime)
    }


    /// Test the composition of the two funcs
    @Test
    fun `#search artist, album`() {
        val p = search("\\a foo \\b album")
        assertNotNull(p.artist)
        assertNotNull(p.album)
        assertNull(p.runtime)
    }
    @Test
    fun `#search artist`() {
        val p = search("\\a foo")
        assertNotNull(p.artist)
        assertNull(p.album)
        assertNull(p.runtime)
    }
    @Test
    fun `#search album`() {
        val p = search("\\b foo")
        assertNull(p.artist)
        assertNotNull(p.album)
        assertNull(p.runtime)
    }
    @Test
    fun `#search runtime, valid time`() {
        val p = search("\\l 10-90")
        assertNull(p.artist)
        assertNull(p.album)
        assertNotNull(p.runtime)
    }
    @Test
    fun `#search runtime, invalid time`() {
        val p = search("\\l foo")
        assertNull(p.artist)
        assertNull(p.album)
        assertNull(p.runtime)
    }
}