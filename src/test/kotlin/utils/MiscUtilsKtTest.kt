package utils

import klarksonmainframe.utils.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MiscUtilsKtTest {

    @Test
    fun `Test Double#format`() {
        assertEquals("10.00", (10.0).format(2))
        assertEquals("10", (10.0).format(0))
        assertEquals("0.00", (0.0).format(2))
        assertEquals("12.346", (12.3456789).format(3))
        assertEquals("-1.01", (-1.0101).format(2))
        assertEquals("-1.02", (-1.024995).format(2))
    }

    @Test
    fun `Test String#splitWithDelims`() {
        assertEquals(
            listOf("1", "+2", "+3"),
            "1+2+3".splitWithDelims("+")
        )

        assertEquals(
            listOf<String>(),
            "".splitWithDelims("/")
        )

        assertEquals(
            listOf<String>(),
            "".splitWithDelims("")
        )

        assertEquals(
            listOf("a", "//b", "//c"),
            "a//b//c".splitWithDelims("//")
        )

        assertEquals(
            listOf("//a"),
            "//a".splitWithDelims("//")
        )
    }

    @Test
    fun `Test String#trimString`() {
        val s = "foobar"

        assertEquals(
            "bar",
            s.trimString("foo")
        )

        assertEquals(
            s,
            s.trimString("")
        )

        assertEquals(
            s,
            s.trimString("bar")
        )
    }

    @Test
    fun `Test median`() {
        assertEquals(
            10,
            median(listOf(0, 10, 20))
        )
        assertEquals(
            10,
            median(listOf(0, 10, 25))
        )
        assertEquals(
            10,
            median(listOf(10, 10, 25))
        )

        assertEquals(
            15,
            median(listOf(10, 20))
        )

        assertEquals(
            15,
            median(listOf(20, 10))
        )

        assertEquals(
            12,
            median(listOf(0, 0, 25, 25))
        )
    }

    @Test
    fun `#extractTrackNumber`() {
        assertNull(extractTrackNumber(""))
        assertNull(extractTrackNumber(null))
        assertNull(extractTrackNumber("Barbaric barbarossa"))

        assertEquals(10, extractTrackNumber(" 10"))
        assertEquals(10, extractTrackNumber("10 "))
        assertEquals(12, extractTrackNumber("12/100"))
        assertEquals(12, extractTrackNumber("12 100"))
    }

    @Test
    fun `#extractDiscNumber`() {
        assertNull(extractDiscNumber(""))
        assertNull(extractDiscNumber(null))
        assertNull(extractDiscNumber("Barbaric barbarossa"))

        assertEquals(2, extractDiscNumber("2"))
        assertEquals(2, extractDiscNumber("2/2"))
        assertEquals(2, extractDiscNumber("2(2)"))
    }

    @Test
    fun `#extractYear`() {
        assertNull(extractYear(""))
        assertNull(extractYear("foobar"))

        assertEquals(2000, extractYear("Sep 1 2000"))
        assertEquals(2000, extractYear("1-1-2000"))
    }
}